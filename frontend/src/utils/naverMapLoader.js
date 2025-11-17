/**
 * 네이버 지도 API를 동적으로 로드하는 유틸리티
 */

let isLoading = false;
let isLoaded = false;

/**
 * 네이버 지도 API 스크립트를 동적으로 로드합니다.
 * @returns {Promise<void>}
 */
export const loadNaverMapScript = () => {
  return new Promise((resolve, reject) => {
    // 이미 로드된 경우
    if (isLoaded) {
      resolve();
      return;
    }

    // 로딩 중인 경우
    if (isLoading) {
      const checkInterval = setInterval(() => {
        if (isLoaded) {
          clearInterval(checkInterval);
          resolve();
        }
      }, 100);
      return;
    }

    isLoading = true;

    // 환경 변수에서 클라이언트 ID 가져오기
    const clientId = import.meta.env.VITE_NAVER_MAP_CLIENT_ID;

    if (!clientId) {
      reject(new Error('네이버 지도 API 클라이언트 ID가 설정되지 않았습니다. .env 파일을 확인하세요.'));
      return;
    }

    // 스크립트 요소 생성
    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpClientId=${clientId}&submodules=geocoder`;
    
    script.onload = () => {
      isLoaded = true;
      isLoading = false;
      console.log('✅ 네이버 지도 API 로드 완료');
      resolve();
    };

    script.onerror = () => {
      isLoading = false;
      reject(new Error('네이버 지도 API 로드 실패'));
    };

    document.head.appendChild(script);
  });
};

/**
 * 네이버 지도 API가 로드되었는지 확인
 * @returns {boolean}
 */
export const isNaverMapLoaded = () => {
  return typeof window.naver !== 'undefined' && typeof window.naver.maps !== 'undefined';
};


