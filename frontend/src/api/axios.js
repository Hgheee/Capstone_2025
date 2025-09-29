import axios from "axios";

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8081/api";

const getToken = () => localStorage.getItem("accessToken");

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,

  withCredentials: false,
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      // 토큰 삭제
      // localStorage.removeItem("accessToken");
      // localStorage.removeItem("user");
      // location.href = "/login";
    }
    return Promise.reject(err);
  }
);

export default api;
