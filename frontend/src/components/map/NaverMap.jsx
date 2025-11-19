import { useEffect, useRef, useState } from 'react';
import { loadNaverMapScript } from '../../utils/naverMapLoader';
import { geocodeLostItemLocation } from '../../utils/geocoding';

/**
 * 네이버 지도를 표시하는 컴포넌트
 * @param {Object} props
 * @param {Object} props.item - 분실물 아이템
 * @param {Object} props.userLocation - 사용자 현재 위치 {lat, lng}
 * @param {string} props.width - 지도 너비 (기본: 100%)
 * @param {string} props.height - 지도 높이 (기본: 400px)
 */
export default function NaverMap({ item, userLocation, width = '100%', height = '400px' }) {
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const [loading, setLoading] = useState(true);
  const [loadingMessage, setLoadingMessage] = useState('지도 로딩 중...');
  const [error, setError] = useState(null);
  const [destination, setDestination] = useState(null);
  const [distance, setDistance] = useState(null);

  useEffect(() => {
    // DOM이 준비될 때까지 대기
    const waitForDOM = () => {
      return new Promise((resolve) => {
        const checkDOM = () => {
          if (mapRef.current) {
            resolve();
          } else {
            requestAnimationFrame(checkDOM);
          }
        };
        checkDOM();
      });
    };

    const initMap = async () => {
      try {
        setLoading(true);
        setError(null);
        setLoadingMessage('지도 준비 중...');

        // 1. DOM 요소가 준비될 때까지 대기 (최대 1초)
        let attempts = 0;
        while (!mapRef.current && attempts < 50) {
          await new Promise(resolve => setTimeout(resolve, 20));
          attempts++;
        }

        if (!mapRef.current) {
          // requestAnimationFrame으로 한 번 더 시도
          await waitForDOM();
        }

        if (!mapRef.current) {
          throw new Error('지도 컨테이너를 찾을 수 없습니다. 페이지를 새로고침해주세요.');
        }

        // 2. 네이버 지도 API 로드와 좌표 가져오기를 병렬 처리 (타임아웃 10초)
        setLoadingMessage('위치 정보 가져오는 중...');
        
        const timeoutPromise = new Promise((_, reject) => {
          setTimeout(() => reject(new Error('지도 로딩 시간 초과 (10초)')), 10000);
        });
        
        try {
          const [_, destCoords] = await Promise.race([
            Promise.all([
              loadNaverMapScript(), // 네이버 지도 API 로드
              geocodeLostItemLocation(item), // 좌표 가져오기
            ]),
            timeoutPromise,
          ]);
          setDestination(destCoords);
        } catch (timeoutError) {
          throw new Error('지도 로딩이 너무 오래 걸립니다. 네이버 지도 API 키를 확인하거나 네트워크 연결을 확인해주세요.');
        }
        
        setLoadingMessage('지도 표시 중...');

        // 4. 지도 생성 (다시 한 번 확인)
        if (!mapRef.current) {
          throw new Error('지도 컨테이너가 사라졌습니다.');
        }

        const mapOptions = {
          center: new naver.maps.LatLng(destCoords.lat, destCoords.lng),
          zoom: 15,
          zoomControl: true,
          zoomControlOptions: {
            position: naver.maps.Position.TOP_RIGHT,
          },
        };

        const map = new naver.maps.Map(mapRef.current, mapOptions);
        mapInstanceRef.current = map;

        // 5. 도착지 마커 (분실물 보관 위치)
        const destMarker = new naver.maps.Marker({
          position: new naver.maps.LatLng(destCoords.lat, destCoords.lng),
          map: map,
          title: item.storageLocation || item.location || '분실물 보관 위치',
          icon: {
            content: `
              <div style="
                background: #22c55e;
                color: white;
                padding: 8px 12px;
                border-radius: 20px;
                font-weight: bold;
                box-shadow: 0 2px 8px rgba(0,0,0,0.3);
                white-space: nowrap;
              ">
                📍 ${item.storageLocation || item.location || '보관 위치'}
              </div>
            `,
            size: new naver.maps.Size(38, 38),
            anchor: new naver.maps.Point(19, 38),
          },
        });

        // 6. 사용자 현재 위치 마커 (있는 경우)
        if (userLocation) {
          const userMarker = new naver.maps.Marker({
            position: new naver.maps.LatLng(userLocation.lat, userLocation.lng),
            map: map,
            title: '내 위치',
            icon: {
              content: `
                <div style="
                  background: #3b82f6;
                  color: white;
                  padding: 8px 12px;
                  border-radius: 20px;
                  font-weight: bold;
                  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
                  white-space: nowrap;
                ">
                  🧭 내 위치
                </div>
              `,
              size: new naver.maps.Size(38, 38),
              anchor: new naver.maps.Point(19, 38),
            },
          });

          // 7. 경로선 그리기 (직선)
          const polyline = new naver.maps.Polyline({
            map: map,
            path: [
              new naver.maps.LatLng(userLocation.lat, userLocation.lng),
              new naver.maps.LatLng(destCoords.lat, destCoords.lng),
            ],
            strokeColor: '#3b82f6',
            strokeOpacity: 0.8,
            strokeWeight: 4,
            strokeStyle: 'solid',
          });

          // 8. 거리 계산
          const R = 6371; // 지구 반지름 (km)
          const dLat = toRad(destCoords.lat - userLocation.lat);
          const dLng = toRad(destCoords.lng - userLocation.lng);
          const a =
            Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(toRad(userLocation.lat)) *
              Math.cos(toRad(destCoords.lat)) *
              Math.sin(dLng / 2) *
              Math.sin(dLng / 2);
          const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
          const dist = R * c;
          setDistance(dist);

          // 9. 두 마커가 모두 보이도록 지도 범위 조정
          const bounds = new naver.maps.LatLngBounds(
            new naver.maps.LatLng(userLocation.lat, userLocation.lng),
            new naver.maps.LatLng(destCoords.lat, destCoords.lng)
          );
          map.fitBounds(bounds, { top: 50, right: 50, bottom: 50, left: 50 });
        }

        setLoading(false);
      } catch (err) {
        console.error('지도 초기화 실패:', err);
        setError(err.message || '지도를 불러올 수 없습니다.');
        setLoading(false);
      }
    };

    initMap();

    // 클린업
    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.destroy();
        mapInstanceRef.current = null;
      }
    };
  }, [item, userLocation]);

  const toRad = (value) => (value * Math.PI) / 180;

  // 네이버 지도로 길찾기
  const openNaverMap = () => {
    if (!destination) return;

    let url = `https://map.naver.com/v5/search/${encodeURIComponent(
      item.storageLocation || item.location || '분실물 보관 위치'
    )}`;

    if (userLocation) {
      // 출발지와 도착지 설정
      url = `https://map.naver.com/v5/directions/${userLocation.lng},${userLocation.lat},현재%20위치/${destination.lng},${destination.lat},${encodeURIComponent(item.storageLocation || item.location)}/-/transit`;
    }

    window.open(url, '_blank');
  };

  if (loading) {
    return (
      <div
        style={{ width, height }}
        className="flex items-center justify-center bg-gray-100 rounded-lg"
      >
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-2"></div>
          <p className="text-gray-600 font-medium">{loadingMessage}</p>
          <p className="text-gray-400 text-sm mt-1">잠시만 기다려주세요...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div
        style={{ width, height }}
        className="flex items-center justify-center bg-red-50 rounded-lg border border-red-200"
      >
        <div className="text-center p-4 max-w-md">
          <p className="text-red-600 font-semibold mb-2">❌ {error}</p>
          <p className="text-sm text-gray-600 mb-4">
            주소: {item.storageLocation || item.location || '정보 없음'}
          </p>
          
          {/* ✅ 네이버 지도로 직접 이동 버튼 */}
          <a
            href={`https://map.naver.com/v5/search/${encodeURIComponent(item.storageLocation || item.location || '분실물 보관 위치')}`}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-block px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg font-semibold transition-colors"
          >
            🗺️ 네이버 지도에서 보기
          </a>
          
          <p className="text-xs text-gray-500 mt-2">
            지도가 로드되지 않으면 위 버튼을 클릭하세요
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="relative">
      <div ref={mapRef} style={{ width, height }} className="rounded-lg shadow-lg" />

      {/* 정보 오버레이 */}
      <div className="absolute top-4 left-4 bg-white rounded-lg shadow-lg p-3 max-w-xs">
        <h3 className="font-bold text-gray-800 mb-1">{item.title}</h3>
        <p className="text-sm text-gray-600 flex items-center gap-1">
          <span>📍</span>
          <span>{item.storageLocation || item.location}</span>
        </p>
        {distance && (
          <p className="text-sm text-blue-600 font-semibold mt-2">
            📏 현재 위치에서 약 {distance.toFixed(2)} km
          </p>
        )}
      </div>

      {/* 네이버 지도 앱으로 길찾기 버튼 */}
      <button
        onClick={openNaverMap}
        className="absolute bottom-4 right-4 bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg shadow-lg flex items-center gap-2 font-semibold transition-colors"
      >
        <span>🗺️</span>
        <span>네이버 지도로 길찾기</span>
      </button>
    </div>
  );
}



