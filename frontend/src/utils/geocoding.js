/**
 * 주소를 좌표로 변환하는 Geocoding 유틸리티
 */

import { isGoogleMapLoaded } from './googleMapLoader';
import { getStationCoordinates, extractStationName } from './stationCoordinates';

// ✅ Geocoding 결과 캐시 (주소 → 좌표)
const geocodeCache = new Map();
const CACHE_EXPIRY = 24 * 60 * 60 * 1000; // 24시간

// ✅ localStorage 키
const STORAGE_KEY = 'geocode_cache';
const MAX_STORAGE_SIZE = 100; // 최대 저장 개수

/**
 * localStorage에서 캐시 로드
 */
const loadCacheFromStorage = () => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const data = JSON.parse(stored);
      const now = Date.now();
      // 유효한 캐시만 메모리에 로드
      Object.entries(data).forEach(([key, value]) => {
        if (now - value.timestamp < CACHE_EXPIRY) {
          geocodeCache.set(key, value);
        }
      });
      if (import.meta.env.DEV && geocodeCache.size > 0) {
        console.log(`💾 localStorage에서 ${geocodeCache.size}개 좌표 캐시 로드`);
      }
    }
  } catch (error) {
    console.warn('캐시 로드 실패:', error);
  }
};

// 초기 로드
if (typeof window !== 'undefined') {
  loadCacheFromStorage();
}

/**
 * 캐시에서 좌표 가져오기 (메모리 + localStorage)
 */
const getCachedCoords = (address) => {
  // 메모리 캐시 확인
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
 * 좌표를 캐시에 저장 (메모리 + localStorage)
 */
const setCachedCoords = (address, coords) => {
  const cacheEntry = {
    coords,
    timestamp: Date.now(),
  };
  
  // 메모리 캐시에 저장
  geocodeCache.set(address, cacheEntry);
  
  // localStorage에 저장
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    const data = stored ? JSON.parse(stored) : {};
    data[address] = cacheEntry;
    
    // 최대 크기 제한
    const keys = Object.keys(data);
    if (keys.length > MAX_STORAGE_SIZE) {
      // 오래된 항목 제거
      const sorted = keys.sort((a, b) => data[a].timestamp - data[b].timestamp);
      sorted.slice(0, keys.length - MAX_STORAGE_SIZE).forEach(key => delete data[key]);
    }
    
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  } catch (error) {
    console.warn('캐시 저장 실패:', error);
  }
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

    if (!isGoogleMapLoaded()) {
      reject(new Error('Google Maps API가 로드되지 않았습니다.'));
      return;
    }

    if (!address || address.trim() === '') {
      reject(new Error('유효한 주소를 입력하세요.'));
      return;
    }

    // Google Geocoder 사용
    const geocoder = new google.maps.Geocoder();
    geocoder.geocode(
      { address: address },
      (results, status) => {
        if (status === 'OK' && results && results.length > 0) {
          const location = results[0].geometry.location;
          const coords = {
            lat: location.lat(),
            lng: location.lng(),
          };

          // ✅ 캐시에 저장
          setCachedCoords(address, coords);

          if (import.meta.env.DEV) {
            console.log(`✅ Geocoding 성공: ${address} → (${coords.lat}, ${coords.lng})`);
          }
          resolve(coords);
        } else {
          reject(new Error(`주소를 찾을 수 없습니다: ${address}`));
        }
      }
    );
  });
};

/**
 * 역 이름에서 좌표를 추출합니다.
 * 1순위: 하드코딩된 역 좌표 사용
 * 2순위: Google Geocoding API 사용
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

  // 2. Google Geocoding API 시도
  // ✅ "서울"을 하드코딩하지 않고 원본 주소 그대로 사용
  // "부산역" → "부산역" 그대로 검색
  // "서울역" → "서울역" 그대로 검색
  let query = stationName.trim();
  
  // "역"이 없으면 추가
  if (!query.endsWith('역')) {
    query = `${query}역`;
  }
  
  // 한국 주소이므로 "대한민국" 추가하여 정확도 향상
  const koreanQuery = `${query}, 대한민국`;
  
  try {
    return await geocodeAddress(koreanQuery);
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
 * 2. Google Geocoding API 사용
 * @param {Object} item - 분실물 아이템
 * @returns {Promise<{lat: number, lng: number}>}
 */
export const geocodeLostItemLocation = async (item) => {
  // ✅ 0. 좌표가 이미 있는 경우 즉시 반환 (가장 빠름)
  if (item.latitude && item.longitude) {
    if (import.meta.env.DEV) {
      console.log('✅ 저장된 좌표 사용:', { lat: item.latitude, lng: item.longitude });
    }
    return { lat: item.latitude, lng: item.longitude };
  }

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

  // 2. 역 좌표를 찾지 못한 경우, Google Geocoding API 시도
  let address = item.storageLocation || item.location;

  if (!address || address.trim() === '') {
    throw new Error('주소 정보를 찾을 수 없습니다.');
  }

  if (import.meta.env.DEV) {
    console.log(`🌐 Google Geocoding API 사용: ${address}`);
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

  // 일반 주소 검색 (한국 주소이므로 "대한민국" 추가)
  const koreanAddress = address.includes('대한민국') ? address : `${address}, 대한민국`;
  try {
    return await geocodeAddress(koreanAddress);
  } catch (error) {
    // "대한민국" 없이 재시도
    try {
      return await geocodeAddress(address);
    } catch (error2) {
      // 모든 방법 실패
      throw new Error(
        `주소를 찾을 수 없습니다.\n` +
        `보관 위치: ${item.storageLocation || '정보 없음'}\n` +
        `습득 장소: ${item.location || '정보 없음'}\n` +
        `제목: ${item.title || '정보 없음'}`
      );
    }
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

