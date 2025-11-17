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
  const [error, setError] = useState(null);
  const [destination, setDestination] = useState(null);
  const [distance, setDistance] = useState(null);

  useEffect(() => {
    const initMap = async () => {
      try {
        setLoading(true);
        setError(null);

        // 1. 네이버 지도 API 로드
        await loadNaverMapScript();

        // 2. 분실물 보관 위치 좌표 가져오기
        const destCoords = await geocodeLostItemLocation(item);
        setDestination(destCoords);

        // 3. 지도 생성
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

        // 4. 도착지 마커 (분실물 보관 위치)
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

        // 5. 사용자 현재 위치 마커 (있는 경우)
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

          // 6. 경로선 그리기 (직선)
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

          // 7. 거리 계산
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

          // 8. 두 마커가 모두 보이도록 지도 범위 조정
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
          <p className="text-gray-600">지도 로딩 중...</p>
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
        <div className="text-center p-4">
          <p className="text-red-600 font-semibold mb-2">❌ {error}</p>
          <p className="text-sm text-gray-600">
            주소: {item.storageLocation || item.location || '정보 없음'}
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


