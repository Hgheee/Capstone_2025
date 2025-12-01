import axios from "axios";

// ✅ Axios 인스턴스 생성
// baseURL은 /api 없이 설정 (모든 API 경로가 /api/로 시작하므로)
// 환경 변수에 /api가 포함되어 있으면 제거
let baseUrlFromEnv = import.meta.env.VITE_API_BASE_URL || "http://localhost:8081";
// 환경 변수에 /api가 포함되어 있으면 제거
if (baseUrlFromEnv.endsWith('/api')) {
  baseUrlFromEnv = baseUrlFromEnv.replace('/api', '');
}
const BASE_URL = baseUrlFromEnv;
const IS_DEV = import.meta.env.DEV;

// 개발 모드에서만 로그 출력
if (IS_DEV) {
  console.log("🔍 [API Config] 원본 VITE_API_BASE_URL:", import.meta.env.VITE_API_BASE_URL);
  console.log("🔍 [API Config] 최종 BASE_URL:", BASE_URL);
  console.log("🔍 [API Config] 예상 전체 URL 예시:", `${BASE_URL}/api/lost-items/recent`);
}

export const api = axios.create({
  baseURL: BASE_URL,  // 🔥 환경변수 대신 직접 지정
  timeout: 300000,  // ✅ 5분 (300초) - 데이터 수집은 시간이 오래 걸림
  withCredentials: true,  // ✅ CORS credentials 허용
  headers: {
    "Content-Type": "application/json",
  },
});

// ✅ 요청 인터셉터 - 로그인/회원가입에는 토큰 제외
api.interceptors.request.use((config) => {
  // 로그인/회원가입 요청에는 토큰을 포함하지 않음
  const isAuthRequest = config.url?.includes('/auth/login') || 
                        config.url?.includes('/auth/signup') ||
                        config.url?.includes('/auth/check-email');
  
  if (!isAuthRequest) {
    const token = localStorage.getItem("token") || localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  
      // 🔍 디버깅: 요청 정보 로깅 (개발 모드만)
      if (IS_DEV) {
        console.log(`[API] ${config.method?.toUpperCase()} ${config.baseURL}${config.url}`);
      }
  
  return config;
});

// 응답 인터셉터 - 에러 처리 개선
api.interceptors.response.use(
  (response) => {
    // 백엔드 ApiResponse 구조: { success: true, data: {...}, error: null }
    if (IS_DEV) {
      console.log(`[API] ✅ ${response.status} ${response.config.method?.toUpperCase()} ${response.config.url}`);
    }
    return response;
  },
  (error) => {
    // 에러 응답 처리
    if (error.response) {
      // 서버가 응답을 반환했지만 2xx 범위를 벗어남
      if (IS_DEV) {
        console.error(`[API] ❌ ${error.response.status} ${error.config?.method?.toUpperCase()} ${error.config?.url}`);
        console.error("API Error:", error.response.data);
      }
    } else if (error.request) {
      // 요청이 전송되었지만 응답을 받지 못함
      if (IS_DEV) {
        console.error("Network Error:", error.request);
      }
    } else {
      // 요청 설정 중 오류 발생
      if (IS_DEV) {
        console.error("Error:", error.message);
      }
    }
    return Promise.reject(error);
  }
);

// 인증 API
export const authApi = {
  signup: (payload) => api.post("/api/auth/signup", payload),
  login: (payload) => api.post("/api/auth/login", payload),
  logout: () => api.post("/api/auth/logout"),
  
  // 아이디 찾기
  findUsername: (data) => api.post("/api/auth/find-username", data),
  
  // 비밀번호 찾기
  forgotPassword: (data) => api.post("/api/auth/forgot-password", data),
  
  // 비밀번호 재설정
  resetPassword: (data) => api.post("/api/auth/reset-password", data),
};

// 분실물 API
export const lostItemApi = {
  // 전체 목록 조회
  list: (params) => api.get("/api/lost-items", { params }),
  
  // 분실물 생성
  create: (payload) => api.post("/api/lost-items", payload),
  
  // 분실물 상세 조회
  getById: (id) => api.get(`/api/lost-items/${id}`),
  
  // 분실물 수정
  update: (id, payload) => api.put(`/api/lost-items/${id}`, payload),
  
  // 분실물 삭제
  delete: (id) => api.delete(`/api/lost-items/${id}`),
  
  // 키워드 검색
  searchByKeyword: (keyword, params) => 
    api.get("/api/lost-items/search", { params: { keyword, ...params } }),
  
  // 지역별 검색
  searchByRegion: (region, params) => 
    api.get("/api/lost-items/search/region", { params: { region, ...params } }),
  
  // 카테고리별 검색
  searchByCategory: (category, params) => 
    api.get(`/api/lost-items/category/${encodeURIComponent(category)}`, { params }),
  
  // 고급 검색 (복합 조건)
  advancedSearch: (searchParams) => 
    api.get("/api/lost-items/search/advanced", { params: searchParams }),
  
  // 전체 텍스트 검색
  searchFullText: (text, params) => 
    api.get("/api/lost-items/search/fulltext", { params: { text, ...params } }),
  
  // 최근 분실물 조회
  getRecent: () => api.get("/api/lost-items/recent"),
  
  // 내 분실물 조회
  getMyItems: (params) => api.get("/api/lost-items/my", { params }),
  
  // 분실물 상태 변경
  updateStatus: (id, status) => 
    api.patch(`/api/lost-items/${id}/status`, null, { params: { status } }),
};

// 인증 토큰 설정
export const setAuthToken = (token) => {
  if (token) {
    api.defaults.headers.common["Authorization"] = `Bearer ${token}`;
    // localStorage에 토큰 저장
    localStorage.setItem("token", token);
  } else {
    delete api.defaults.headers.common["Authorization"];
    // localStorage에서 토큰 제거
    localStorage.removeItem("token");
  }
};

// 초기화 시 저장된 토큰 복원
const savedToken = localStorage.getItem("token");
if (savedToken) {
  setAuthToken(savedToken);
}

export default api;
