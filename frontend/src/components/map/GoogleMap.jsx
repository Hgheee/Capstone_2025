import { useEffect, useRef, useState } from 'react';
import { loadGoogleMapScript } from '../../utils/googleMapLoader';
import { geocodeLostItemLocation } from '../../utils/geocoding';

/**
 * Google 지도를 표시하는 컴포넌트
 * @param {Object} props
 * @param {Object} props.item - 분실물 아이템
 * @param {Object} props.userLocation - 사용자 현재 위치 {lat, lng}
 * @param {string} props.width - 지도 너비 (기본: 100%)
 * @param {string} props.height - 지도 높이 (기본: 400px)
 */
export default function GoogleMap({ item, userLocation, width = '100%', height = '400px' }) {
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markersRef = useRef([]);
  const userMarkerAddedRef = useRef(false); // 사용자 마커 추가 여부 추적
  const [loading, setLoading] = useState(true);
  const [loadingMessage, setLoadingMessage] = useState('지도 로딩 중...');
  const [error, setError] = useState(null);
  const [destination, setDestination] = useState(null);
  const [distance, setDistance] = useState(null);
  const [useEmbedMap, setUseEmbedMap] = useState(false); // Embed 지도 사용 여부

  useEffect(() => {
    const initMap = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // ✅ 1. 좌표가 이미 있는 경우 Geocoding 스킵 (성능 개선)
        let destCoords = null;
        if (item.latitude && item.longitude) {
          destCoords = {
            lat: item.latitude,
            lng: item.longitude,
          };
          setDestination(destCoords);
          if (import.meta.env.DEV) {
            console.log('✅ 저장된 좌표 사용:', destCoords);
          }
        }

        // 2. DOM 요소 확인 (더 안정적인 대기)
        let domAttempts = 0;
        while (!mapRef.current && domAttempts < 10) {
          await new Promise(resolve => {
            requestAnimationFrame(() => {
              setTimeout(resolve, 10);
            });
          });
          domAttempts++;
        }

        if (!mapRef.current) {
          // DOM을 찾지 못하면 Embed 지도로 폴백
          console.warn('지도 컨테이너를 찾을 수 없어 Embed 지도로 전환합니다.');
          setUseEmbedMap(true);
          setLoading(false);
          return;
        }

        // 3. Google Maps API 로드
        setLoadingMessage('지도 로딩 중...');
        await loadGoogleMapScript();

        // 4. 좌표가 없으면 Geocoding 수행
        if (!destCoords) {
          setLoadingMessage('위치 정보 가져오는 중...');
          try {
            destCoords = await geocodeLostItemLocation(item);
            setDestination(destCoords);
          } catch (geocodeError) {
            throw new Error(`위치 정보를 가져올 수 없습니다: ${geocodeError.message}`);
          }
        }
        
        setLoadingMessage('지도 표시 중...');

        // 5. 지도 생성 (최적화된 옵션)
        const mapOptions = {
          center: { lat: destCoords.lat, lng: destCoords.lng },
          zoom: 15,
          zoomControl: true,
          mapTypeControl: false,
          streetViewControl: false,
          fullscreenControl: true,
          disableDefaultUI: false,
          // 성능 최적화 옵션
          optimizeForMobile: true,
        };

        const map = new google.maps.Map(mapRef.current, mapOptions);
        mapInstanceRef.current = map;

        // 기존 마커 제거
        markersRef.current.forEach(marker => marker.setMap(null));
        markersRef.current = [];

        // 6. 도착지 마커 추가 (즉시 표시)
        const destMarker = new google.maps.Marker({
          position: { lat: destCoords.lat, lng: destCoords.lng },
          map: map,
          title: item.storageLocation || item.location || '분실물 보관 위치',
          icon: {
            path: google.maps.SymbolPath.CIRCLE,
            scale: 10,
            fillColor: '#22c55e',
            fillOpacity: 1,
            strokeColor: '#ffffff',
            strokeWeight: 2,
          },
        });
        markersRef.current.push(destMarker);

        // 정보창 생성
        const destInfoWindow = new google.maps.InfoWindow({
          content: `
            <div style="padding: 8px;">
              <strong style="color: #22c55e;">📍 ${item.storageLocation || item.location || '보관 위치'}</strong>
            </div>
          `,
        });

        destMarker.addListener('click', () => {
          destInfoWindow.open(map, destMarker);
        });

        // ✅ 지도를 먼저 표시하고 사용자 위치는 나중에 추가
        setLoading(false);

        // 7. 사용자 위치가 이미 있으면 추가 (초기 로드 시)
        if (userLocation) {
          // 다음 프레임에서 사용자 위치 마커 추가
          requestAnimationFrame(() => {
            addUserLocationMarker(map, userLocation, destCoords);
          });
        } else {
          // 사용자 위치가 없으면 목적지만 중심으로
          map.setCenter({ lat: destCoords.lat, lng: destCoords.lng });
        }
      } catch (err) {
        console.error('지도 초기화 실패:', err);
        // 에러 발생 시 Embed 지도로 폴백
        setUseEmbedMap(true);
        setError(null);
        setLoading(false);
      }
    };

    initMap();

    // 클린업
    return () => {
      // 마커 제거
      if (markersRef.current) {
        markersRef.current.forEach(marker => marker.setMap(null));
        markersRef.current = [];
      }
      // 지도 인스턴스는 자동으로 정리됨
      mapInstanceRef.current = null;
      userMarkerAddedRef.current = false;
    };
  }, [item]);

  // 사용자 위치가 업데이트될 때 마커 추가
  useEffect(() => {
    if (userLocation && mapInstanceRef.current && destination && !userMarkerAddedRef.current) {
      addUserLocationMarker(mapInstanceRef.current, userLocation, destination);
      userMarkerAddedRef.current = true;
    }
  }, [userLocation, destination]);

  // 사용자 위치 마커 추가 함수 (비동기)
  const addUserLocationMarker = (map, userLocation, destCoords) => {
    if (!map || !userLocation || !destCoords || userMarkerAddedRef.current) return;

    const userMarker = new google.maps.Marker({
      position: { lat: userLocation.lat, lng: userLocation.lng },
      map: map,
      title: '내 위치',
      icon: {
        path: google.maps.SymbolPath.CIRCLE,
        scale: 10,
        fillColor: '#3b82f6',
        fillOpacity: 1,
        strokeColor: '#ffffff',
        strokeWeight: 2,
      },
    });
    markersRef.current.push(userMarker);

    const userInfoWindow = new google.maps.InfoWindow({
      content: `
        <div style="padding: 8px;">
          <strong style="color: #3b82f6;">🧭 내 위치</strong>
        </div>
      `,
    });

    userMarker.addListener('click', () => {
      userInfoWindow.open(map, userMarker);
    });

    // 경로선 그리기 (직선)
    const polyline = new google.maps.Polyline({
      path: [
        { lat: userLocation.lat, lng: userLocation.lng },
        { lat: destCoords.lat, lng: destCoords.lng },
      ],
      geodesic: true,
      strokeColor: '#3b82f6',
      strokeOpacity: 0.8,
      strokeWeight: 4,
    });
    polyline.setMap(map);

    // 거리 계산
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

    // 두 마커가 모두 보이도록 지도 범위 조정
    const bounds = new google.maps.LatLngBounds();
    bounds.extend({ lat: userLocation.lat, lng: userLocation.lng });
    bounds.extend({ lat: destCoords.lat, lng: destCoords.lng });
    map.fitBounds(bounds, { top: 50, right: 50, bottom: 50, left: 50 });
    
    userMarkerAddedRef.current = true;
  };

  const toRad = (value) => (value * Math.PI) / 180;

  // Google 지도로 길찾기
  const openGoogleMap = () => {
    if (!destination) return;

    const destAddress = encodeURIComponent(item.storageLocation || item.location || '분실물 보관 위치');
    let url = `https://www.google.com/maps/search/?api=1&query=${destAddress}`;

    if (userLocation) {
      // 출발지와 도착지 설정
      url = `https://www.google.com/maps/dir/?api=1&origin=${userLocation.lat},${userLocation.lng}&destination=${destination.lat},${destination.lng}&travelmode=transit`;
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

  // Embed 지도 사용 (에러 발생 시 또는 직접 선택)
  if (useEmbedMap || error) {
    const address = item.storageLocation || item.location || '분실물 보관 위치';
    const embedUrl = `https://www.google.com/maps/embed/v1/place?key=${import.meta.env.VITE_GOOGLE_MAP_API_KEY || ''}&q=${encodeURIComponent(address)}`;
    
    // API 키가 없으면 일반 검색 URL 사용
    const fallbackUrl = `https://www.google.com/maps?q=${encodeURIComponent(address)}&output=embed`;
    const mapUrl = import.meta.env.VITE_GOOGLE_MAP_API_KEY ? embedUrl : fallbackUrl;
    
    return (
      <div className="relative" style={{ width, height }}>
        <iframe
          width="100%"
          height="100%"
          style={{ border: 0, borderRadius: '0.5rem' }}
          loading="lazy"
          allowFullScreen
          referrerPolicy="no-referrer-when-downgrade"
          src={mapUrl}
          title={`${address} 지도`}
        />
        
        {/* 정보 오버레이 */}
        <div className="absolute top-4 left-4 bg-white rounded-lg shadow-lg p-3 max-w-xs z-10">
          <h3 className="font-bold text-gray-800 mb-1">{item.title}</h3>
          <p className="text-sm text-gray-600 flex items-center gap-1">
            <span>📍</span>
            <span>{address}</span>
          </p>
        </div>

        {/* Google 지도 앱으로 길찾기 버튼 */}
        <a
          href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address)}`}
          target="_blank"
          rel="noopener noreferrer"
          className="absolute bottom-4 right-4 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg shadow-lg flex items-center gap-2 font-semibold transition-colors z-10"
        >
          <span>🗺️</span>
          <span>Google 지도에서 열기</span>
        </a>
      </div>
    );
  }

  return (
    <div className="relative">
      <div ref={mapRef} style={{ width, height }} className="rounded-lg shadow-lg" />

      {/* 정보 오버레이 */}
      <div className="absolute top-4 left-4 bg-white rounded-lg shadow-lg p-3 max-w-xs z-10">
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

      {/* Google 지도 앱으로 길찾기 버튼 */}
      <button
        onClick={openGoogleMap}
        className="absolute bottom-4 right-4 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg shadow-lg flex items-center gap-2 font-semibold transition-colors z-10"
      >
        <span>🗺️</span>
        <span>Google 지도로 길찾기</span>
      </button>
    </div>
  );
}

