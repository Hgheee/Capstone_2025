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
      const errorMsg =
        "네이버 지도 API 클라이언트 ID가 설정되지 않았습니다.\n\n해결 방법:\n1. frontend/.env 파일 생성\n2. VITE_NAVER_MAP_CLIENT_ID=your_client_id 추가\n3. 프론트엔드 서버 재시작\n\n자세한 내용: 네이버지도_API키_설정가이드.md 참고";
      console.error("❌ 네이버 지도 API 키 없음:", errorMsg);
      reject(new Error(errorMsg));
      return;
    }

    if (import.meta.env.DEV) {
      console.log(
        "🔑 네이버 지도 API 키 확인됨:",
        clientId.substring(0, 10) + "..."
      );
    }

    // 스크립트 요소 생성
    const script = document.createElement("script");
    script.type = "text/javascript";
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpClientId=${clientId}&submodules=geocoder`;

    script.onload = () => {
      // API가 실제로 로드되었는지 확인
      if (
        typeof window.naver === "undefined" ||
        typeof window.naver.maps === "undefined"
      ) {
        isLoading = false;
        reject(
          new Error(
            "네이버 지도 API가 로드되었지만 사용할 수 없습니다. API 키를 확인하세요."
          )
        );
        return;
      }

      isLoaded = true;
      isLoading = false;
      if (import.meta.env.DEV) {
        console.log("✅ 네이버 지도 API 로드 완료");
      }
      resolve();
    };

    script.onerror = (error) => {
      isLoading = false;
      console.error("❌ 네이버 지도 API 스크립트 로드 실패:", error);
      reject(
        new Error(
          "네이버 지도 API 로드 실패. 네트워크 연결 또는 API 키를 확인하세요."
        )
      );
    };

    // 타임아웃 추가 (15초)
    setTimeout(() => {
      if (isLoading) {
        isLoading = false;
        reject(
          new Error(
            "네이버 지도 API 로드 시간 초과 (15초). 네트워크 연결을 확인하세요."
          )
        );
      }
    }, 15000);

    document.head.appendChild(script);
  });
};

/**
 * 네이버 지도 API가 로드되었는지 확인
 * @returns {boolean}
 */
export const isNaverMapLoaded = () => {
  return (
    typeof window.naver !== "undefined" &&
    typeof window.naver.maps !== "undefined"
  );
};
