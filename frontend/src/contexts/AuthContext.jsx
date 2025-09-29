import { createContext, useContext, useEffect, useMemo, useState } from "react";
import api from "../api/axios";

const AuthContext = createContext(null);

export function useAuth() {
  return useContext(AuthContext);
}

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null); // { id, username, ... }
  const [token, setToken] = useState(null); // accessToken (JWT)
  const [loading, setLoading] = useState(true);

  // 초기 로드: 로컬스토리지에서 복원
  useEffect(() => {
    const savedToken = localStorage.getItem("accessToken");
    const savedUser = localStorage.getItem("user");
    if (savedToken) setToken(savedToken);
    if (savedUser) setUser(JSON.parse(savedUser));
    setLoading(false);
  }, []);

  // 로그인
  const login = async ({ username, password }) => {
    // 백엔드 형식에 맞게 변경하세요.
    const { data } = await api.post("/auth/login", { username, password });
    // 백엔드에서 반환하는 키 이름에 맞게 수정
    const accessToken = data.accessToken || data.token;
    const userInfo = data.user || { username };

    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("user", JSON.stringify(userInfo));

    setToken(accessToken);
    setUser(userInfo);
    return userInfo;
  };

  // 회원가입
  const register = async (payload) => {
    // 예1) /auth/signup  예2) /auth/register
    const { data } = await api.post("/auth/signup", payload);
    return data;
  };

  // 로그아웃
  const logout = () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("user");
    setToken(null);
    setUser(null);
    // 필요하면 서버에도 로그아웃 알리기: api.post("/auth/logout")
  };

  const value = useMemo(
    () => ({ user, token, loading, login, register, logout }),
    [user, token, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
