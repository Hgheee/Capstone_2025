import { createContext, useContext, useEffect, useMemo, useState } from "react";
import api from "../api/axios";

const AuthContext = createContext(null);

export function useAuth() {
  return useContext(AuthContext);
}

/** 휴대폰 번호 자동 하이픈 (010-XXXX-XXXX / 02-XXXX-XXXX 지원) */
function formatPhoneKR(input) {
  const digits = (input ?? "").replace(/\D/g, "");
  if (!digits) return "";
  if (digits.startsWith("02")) {
    // 02-XXXX-XXXX
    if (digits.length <= 2) return digits;
    if (digits.length <= 6) return digits.replace(/(\d{2})(\d{0,4})/, "$1-$2");
    return digits.replace(/(\d{2})(\d{4})(\d{0,4}).*/, "$1-$2-$3");
  }
  // 010/011/016/017/018/019
  if (digits.length <= 3) return digits;
  if (digits.length <= 7) return digits.replace(/(\d{3})(\d{0,4})/, "$1-$2");
  return digits.replace(/(\d{3})(\d{3,4})(\d{0,4}).*/, "$1-$2-$3");
}

/** 백엔드 ApiResponse({ success, data, error })와 direct body를 모두 호환 */
function unwrapApi(data) {
  return data && typeof data === "object" && "data" in data ? data.data : data;
}

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null); // { id, username, email, name, ... }
  const [token, setToken] = useState(null); // accessToken (JWT)
  const [loading, setLoading] = useState(true);

  // 초기 로드: 로컬스토리지에서 복원
  useEffect(() => {
    const savedToken = localStorage.getItem("accessToken");
    const savedUser = localStorage.getItem("user");
    if (savedToken) setToken(savedToken);
    if (savedUser) {
      try {
        setUser(JSON.parse(savedUser));
      } catch {
        /* noop */
      }
    }
    setLoading(false);
  }, []);

  // 로그인: 현재 구조 유지(아이디= username, 비번= password)
  // 백엔드가 ApiResponse 래핑을 쓰는 경우를 고려해 파싱
  const login = async ({ username, password, email }) => {
    try {
      // 혹시 이메일로 로그인 폼을 구성했다면 email도 함께 전송(백엔드 무시해도 됨)
      const payload = { username, password, ...(email ? { email } : {}) };

      const { data } = await api.post("/auth/login", payload, {
        headers: { "Content-Type": "application/json" },
        timeout: 15000,
      });

      const body = unwrapApi(data);
      const accessToken = body?.accessToken || body?.token;
      const userInfo = body?.user || { username, email };

      if (!accessToken) {
        throw new Error("로그인 토큰이 응답에 없습니다.");
      }

      localStorage.setItem("accessToken", accessToken);
      localStorage.setItem("user", JSON.stringify(userInfo));
      setToken(accessToken);
      setUser(userInfo);
      return userInfo;
    } catch (err) {
      // 에러 메시지 최대한 뽑아서 사용자에게 전달
      const msg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        "로그인에 실패했습니다.";
      throw new Error(msg);
    }
  };

  // 회원가입: 현재 구조 유지(이메일, 아이디 분리) + phone 자동 하이픈 적용
  const register = async (payload) => {
    try {
      // 기대 키: { email, username, name, password, phone? }
      const clean = {
        email: payload?.email?.trim(),
        username: payload?.username?.trim(),
        name: payload?.name?.trim(),
        password: payload?.password,
        // phone은 백엔드 정규식(010-1234-5678) 통과하도록 하이픈 포맷
        ...(payload?.phone !== undefined
          ? { phone: formatPhoneKR(payload.phone) }
          : {}),
      };

      const { data } = await api.post("/auth/signup", clean, {
        headers: { "Content-Type": "application/json" },
        timeout: 15000,
      });

      // 보통 회원가입은 토큰을 바로 주지 않으니 반환만
      return unwrapApi(data);
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        "회원가입에 실패했습니다. 입력값을 다시 확인해주세요.";
      throw new Error(msg);
    }
  };

  const logout = () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("user");
    setToken(null);
    setUser(null);
    // 필요시: api.post("/auth/logout");
  };

  const value = useMemo(
    () => ({ user, token, loading, login, register, logout }),
    [user, token, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
