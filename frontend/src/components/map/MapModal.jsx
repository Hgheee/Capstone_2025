import { useEffect, useState } from 'react';
import NaverMap from './NaverMap';

/**
 * 지도를 표시하는 모달 컴포넌트
 * @param {Object} props
 * @param {boolean} props.isOpen - 모달 열림 상태
 * @param {Function} props.onClose - 모달 닫기 함수
 * @param {Object} props.item - 분실물 아이템
 */
export default function MapModal({ isOpen, onClose, item }) {
  const [userLocation, setUserLocation] = useState(null);
  const [locationError, setLocationError] = useState(null);
  const [isLoadingLocation, setIsLoadingLocation] = useState(false);
  const [isMapReady, setIsMapReady] = useState(false); // ✅ 지도 렌더링 준비 상태

  // 모달이 열릴 때 지도 렌더링 준비
  useEffect(() => {
    if (isOpen) {
      // 모달이 완전히 열린 후 지도 렌더링 (DOM이 준비될 때까지 대기)
      // ✅ 50ms로 단축 (더 빠른 반응)
      const timer = setTimeout(() => {
        setIsMapReady(true);
      }, 50);
      return () => clearTimeout(timer);
    } else {
      setIsMapReady(false); // 모달이 닫히면 지도도 제거
    }
  }, [isOpen]);

  // 사용자 현재 위치 가져오기
  useEffect(() => {
    if (isOpen && !userLocation) {
      getUserLocation();
    }
  }, [isOpen]);

  const getUserLocation = () => {
    if (!navigator.geolocation) {
      setLocationError('이 브라우저는 위치 서비스를 지원하지 않습니다.');
      return;
    }

    setIsLoadingLocation(true);
    setLocationError(null);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setUserLocation({
          lat: position.coords.latitude,
          lng: position.coords.longitude,
        });
        setIsLoadingLocation(false);
      },
      (error) => {
        if (import.meta.env.DEV) {
          console.error('위치 정보 가져오기 실패:', error);
        }
        setLocationError('위치 정보를 가져올 수 없습니다. 위치 권한을 확인해주세요.');
        setIsLoadingLocation(false);
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      }
    );
  };

  // ESC 키로 모달 닫기
  useEffect(() => {
    const handleEsc = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleEsc);
    }

    return () => {
      document.removeEventListener('keydown', handleEsc);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-xl shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div className="bg-gradient-to-r from-green-600 to-blue-600 text-white p-4 flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold">{item.title}</h2>
            <p className="text-sm opacity-90 mt-1">
              📍 {item.storageLocation || item.location || '위치 정보 없음'}
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-white hover:bg-white hover:bg-opacity-20 rounded-full p-2 transition-colors"
            aria-label="닫기"
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* 지도 영역 */}
        <div className="p-4">
          {isLoadingLocation && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-4 flex items-center gap-2">
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600"></div>
              <span className="text-blue-700">현재 위치 확인 중...</span>
            </div>
          )}

          {locationError && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 mb-4">
              <p className="text-yellow-800 text-sm">⚠️ {locationError}</p>
              <p className="text-yellow-700 text-xs mt-1">
                지도는 분실물 보관 위치만 표시됩니다.
              </p>
              <button
                onClick={getUserLocation}
                className="mt-2 text-blue-600 text-sm underline hover:text-blue-800"
              >
                다시 시도
              </button>
            </div>
          )}

          {/* ✅ 지도가 준비되었을 때만 렌더링 */}
          {isMapReady ? (
            <NaverMap item={item} userLocation={userLocation} height="500px" />
          ) : (
            <div className="h-[500px] flex items-center justify-center bg-gray-100 rounded-lg">
              <div className="text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-2"></div>
                <p className="text-gray-600">지도 준비 중...</p>
              </div>
            </div>
          )}
        </div>

        {/* 분실물 정보 */}
        <div className="border-t p-4 bg-gray-50">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-gray-600">습득일:</span>
              <span className="ml-2 font-semibold">
                {item.foundDate || '정보 없음'}
              </span>
            </div>
            <div>
              <span className="text-gray-600">상태:</span>
              <span className="ml-2 font-semibold">{item.status || '정보 없음'}</span>
            </div>
            <div>
              <span className="text-gray-600">카테고리:</span>
              <span className="ml-2 font-semibold">{item.category || '기타'}</span>
            </div>
            <div>
              <span className="text-gray-600">데이터 소스:</span>
              <span className="ml-2 font-semibold">
                {item.dataSource === 'SEOUL_LOST' ? '서울교통공사' : 'LOST112'}
              </span>
            </div>
          </div>

          {item.description && (
            <div className="mt-4">
              <span className="text-gray-600 block mb-1">상세 설명:</span>
              <p className="text-gray-800 bg-white p-2 rounded border">{item.description}</p>
            </div>
          )}
        </div>

        {/* 푸터 */}
        <div className="border-t p-4 flex justify-end gap-2">
          <button
            onClick={onClose}
            className="px-6 py-2 bg-gray-200 hover:bg-gray-300 text-gray-800 rounded-lg font-semibold transition-colors"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  );
}



