/**
 * Google Maps API를 동적으로 로드하는 유틸리티
 */

let isLoading = false;
let isLoaded = false;

/**
 * Google Maps API 스크립트를 동적으로 로드합니다.
 * @returns {Promise<void>}
 */
export const loadGoogleMapScript = () => {
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

    // 환경 변수에서 API 키 가져오기
    const apiKey = import.meta.env.VITE_GOOGLE_MAP_API_KEY;

    if (!apiKey) {
      const errorMsg =
        "Google Maps API 키가 설정되지 않았습니다.\n\n해결 방법:\n1. frontend/.env 파일 생성\n2. VITE_GOOGLE_MAP_API_KEY=your_api_key 추가\n3. 프론트엔드 서버 재시작\n\nGoogle Maps API 키 발급:\nhttps://console.cloud.google.com/google/maps-apis";
      console.error("❌ Google Maps API 키 없음:", errorMsg);
      reject(new Error(errorMsg));
      return;
    }

    if (import.meta.env.DEV) {
      console.log(
        "🔑 Google Maps API 키 확인됨:",
        apiKey.substring(0, 10) + "..."
      );
    }

    // 스크립트 요소 생성
    const script = document.createElement("script");
    script.type = "text/javascript";
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=places,geometry`;
    script.async = true;
    script.defer = true;

    script.onload = () => {
      // API가 실제로 로드되었는지 확인
      if (
        typeof window.google === "undefined" ||
        typeof window.google.maps === "undefined"
      ) {
        isLoading = false;
        reject(
          new Error(
            "Google Maps API가 로드되었지만 사용할 수 없습니다. API 키를 확인하세요."
          )
        );
        return;
      }

      isLoaded = true;
      isLoading = false;
      if (import.meta.env.DEV) {
        console.log("✅ Google Maps API 로드 완료");
      }
      resolve();
    };

    script.onerror = (error) => {
      isLoading = false;
      console.error("❌ Google Maps API 스크립트 로드 실패:", error);
      reject(
        new Error(
          "Google Maps API 로드 실패. 네트워크 연결 또는 API 키를 확인하세요."
        )
      );
    };

    // 타임아웃 추가 (15초)
    setTimeout(() => {
      if (isLoading) {
        isLoading = false;
        reject(
          new Error(
            "Google Maps API 로드 시간 초과 (15초). 네트워크 연결을 확인하세요."
          )
        );
      }
    }, 15000);

    document.head.appendChild(script);
  });
};

/**
 * Google Maps API가 로드되었는지 확인
 * @returns {boolean}
 */
export const isGoogleMapLoaded = () => {
  return (
    typeof window.google !== "undefined" &&
    typeof window.google.maps !== "undefined"
  );
};

