import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // 예: http://localhost:8080
  // withCredentials: true, // 쿠키 인증 쓸 때만 켜기
});

export const authApi = {
  signup: (payload) => api.post("/api/auth/signup", payload),
  login: (payload) => api.post("/api/auth/login", payload),
};

export const lostItemApi = {
  list: () => api.get("/api/lost-items"),
  create: (payload) => api.post("/api/lost-items", payload),
};
