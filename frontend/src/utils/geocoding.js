/**
 * 주소를 좌표로 변환하는 Geocoding 유틸리티
 */

import { isNaverMapLoaded } from './naverMapLoader';
import { getStationCoordinates, extractStationName } from './stationCoordinates';

// ✅ Geocoding 결과 캐시 (주소 → 좌표)
const geocodeCache = new Map();
const CACHE_EXPIRY = 24 * 60 * 60 * 1000; // 24시간

/**
 * 캐시에서 좌표 가져오기
 */
const getCachedCoords = (address) => {
  const cached = geocodeCache.get(address);
  if (cached && Date.now() - cached.timestamp < CACHE_EXPIRY) {
    if (import.meta.env.DEV) {
      console.log(`💾 캐시에서 좌표 사용: ${address}`);
    }
    return cached.coords;
  }
  return null;
};

/**
 * 좌표를 캐시에 저장
 */
const setCachedCoords = (address, coords) => {
  geocodeCache.set(address, {
    coords,
    timestamp: Date.now(),
  });
};

/**
 * 주소를 좌표(위도, 경도)로 변환합니다.
 * @param {string} address - 변환할 주소
 * @returns {Promise<{lat: number, lng: number}>}
 */
export const geocodeAddress = (address) => {
  return new Promise((resolve, reject) => {
    // ✅ 캐시 확인
    const cached = getCachedCoords(address);
    if (cached) {
      resolve(cached);
      return;
    }

    if (!isNaverMapLoaded()) {
      reject(new Error('네이버 지도 API가 로드되지 않았습니다.'));
      return;
    }

    if (!address || address.trim() === '') {
      reject(new Error('유효한 주소를 입력하세요.'));
      return;
    }

    // 네이버 Geocoder 사용
    naver.maps.Service.geocode(
      {
        query: address,
      },
      (status, response) => {
        if (status === naver.maps.Service.Status.ERROR) {
          reject(new Error('Geocoding 실패: 주소를 찾을 수 없습니다.'));
          return;
        }

        if (response.v2.meta.totalCount === 0) {
          reject(new Error(`주소를 찾을 수 없습니다: ${address}`));
          return;
        }

        // 첫 번째 결과 사용
        const item = response.v2.addresses[0];
        const coords = {
          lat: parseFloat(item.y),
          lng: parseFloat(item.x),
        };

      // ✅ 캐시에 저장
      setCachedCoords(address, coords);

      if (import.meta.env.DEV) {
        console.log(`✅ Geocoding 성공: ${address} → (${coords.lat}, ${coords.lng})`);
      }
      resolve(coords);
      }
    );
  });
};

/**
 * 역 이름에서 좌표를 추출합니다.
 * 1순위: 하드코딩된 역 좌표 사용
 * 2순위: 네이버 Geocoding API 사용
 * @param {string} stationName - 역 이름
 * @returns {Promise<{lat: number, lng: number}>}
 */
export const geocodeStation = async (stationName) => {
  // 1. 하드코딩된 역 좌표에서 먼저 찾기
  const coords = getStationCoordinates(stationName);
  if (coords) {
    if (import.meta.env.DEV) {
      console.log(`✅ 역 좌표 매핑 사용: ${stationName} → (${coords.lat}, ${coords.lng})`);
    }
    return { lat: coords.lat, lng: coords.lng };
  }

  // 2. 네이버 Geocoding API 시도
  // ✅ "서울"을 하드코딩하지 않고 원본 주소 그대로 사용
  // "부산역" → "부산역" 그대로 검색
  // "서울역" → "서울역" 그대로 검색
  let query = stationName.trim();
  
  // "역"이 없으면 추가
  if (!query.endsWith('역')) {
    query = `${query}역`;
  }
  
  try {
    return await geocodeAddress(query);
    } catch (error) {
      if (import.meta.env.DEV) {
        console.warn(`역 이름으로 검색 실패: ${stationName}, 원본 주소로 재시도`);
      }
      // 원본 주소 그대로 재시도
      try {
        return await geocodeAddress(stationName);
      } catch (error2) {
        if (import.meta.env.DEV) {
          console.warn(`역 검색 완전 실패: ${stationName}`);
        }
        throw new Error(`역 좌표를 찾을 수 없습니다: ${stationName}`);
      }
    }
};

/**
 * 분실물 보관 위치에서 좌표를 추출합니다.
 * 우선순위:
 * 1. storageLocation, location, title에서 역 이름 추출 → 역 좌표 매핑
 * 2. 네이버 Geocoding API 사용
 * @param {Object} item - 분실물 아이템
 * @returns {Promise<{lat: number, lng: number}>}
 */
export const geocodeLostItemLocation = async (item) => {
  if (import.meta.env.DEV) {
    console.log('🔍 분실물 위치 정보:', {
      storageLocation: item.storageLocation,
      location: item.location,
      title: item.title,
    });
  }

  // 1. storageLocation, location, title에서 역 이름 추출 시도
  const sources = [item.storageLocation, item.location, item.title];
  
  for (const source of sources) {
    if (!source) continue;
    
    // 역 이름 추출
    const stationName = extractStationName(source);
    if (stationName) {
      if (import.meta.env.DEV) {
        console.log(`📍 역 이름 발견: ${stationName} (출처: ${source})`);
      }
      
      // 역 좌표 매핑에서 찾기
      const coords = getStationCoordinates(stationName);
      if (coords) {
        if (import.meta.env.DEV) {
          console.log(`✅ 역 좌표 매핑 성공: ${stationName} → (${coords.lat}, ${coords.lng})`);
        }
        return { lat: coords.lat, lng: coords.lng };
      }
    }
  }

  // 2. 역 좌표를 찾지 못한 경우, 네이버 Geocoding API 시도
  let address = item.storageLocation || item.location;

  if (!address || address.trim() === '') {
    throw new Error('주소 정보를 찾을 수 없습니다.');
  }

  if (import.meta.env.DEV) {
    console.log(`🌐 네이버 Geocoding API 사용: ${address}`);
  }

  // "역"이 포함되어 있으면 역 검색 시도
  if (address.includes('역')) {
    try {
      return await geocodeStation(address);
    } catch (error) {
      if (import.meta.env.DEV) {
        console.warn('역 검색 실패, 일반 주소로 재시도:', error.message);
      }
    }
  }

  // 일반 주소 검색
  try {
    return await geocodeAddress(address);
  } catch (error) {
    // 모든 방법 실패
    throw new Error(
      `주소를 찾을 수 없습니다.\n` +
      `보관 위치: ${item.storageLocation || '정보 없음'}\n` +
      `습득 장소: ${item.location || '정보 없음'}\n` +
      `제목: ${item.title || '정보 없음'}`
    );
  }
};

/**
 * 두 좌표 사이의 거리를 계산합니다 (단위: km)
 * Haversine 공식 사용
 * @param {Object} coord1 - {lat, lng}
 * @param {Object} coord2 - {lat, lng}
 * @returns {number} 거리 (km)
 */
export const calculateDistance = (coord1, coord2) => {
  const R = 6371; // 지구 반지름 (km)
  const dLat = toRad(coord2.lat - coord1.lat);
  const dLng = toRad(coord2.lng - coord1.lng);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(coord1.lat)) *
      Math.cos(toRad(coord2.lat)) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  const distance = R * c;

  return distance;
};

const toRad = (value) => {
  return (value * Math.PI) / 180;
};

