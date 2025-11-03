import axios from "axios";

// Axios 인스턴스 생성
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

// 응답 인터셉터 - 에러 처리 개선
api.interceptors.response.use(
  (response) => {
    // 백엔드 ApiResponse 구조: { success: true, data: {...}, error: null }
    return response;
  },
  (error) => {
    // 에러 응답 처리
    if (error.response) {
      // 서버가 응답을 반환했지만 2xx 범위를 벗어남
      console.error("API Error:", error.response.data);
    } else if (error.request) {
      // 요청이 전송되었지만 응답을 받지 못함
      console.error("Network Error:", error.request);
    } else {
      // 요청 설정 중 오류 발생
      console.error("Error:", error.message);
    }
    return Promise.reject(error);
  }
);

// 인증 API
export const authApi = {
  signup: (payload) => api.post("/api/auth/signup", payload),
  login: (payload) => api.post("/api/auth/login", payload),
  logout: () => api.post("/api/auth/logout"),
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
