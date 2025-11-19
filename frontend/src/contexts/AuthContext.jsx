import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { api, authApi } from "../lib/api";

const AuthContext = createContext(null);

export function useAuth() {
  return useContext(AuthContext);
}

/** 휴대폰 번호 자동 하이픈 (010-XXXX-XXXX / 02-XXXX-XXXX 지원) */
function formatPhoneKR(input) {
  const digits = (input ?? "").replace(/\D/g, "");
  if (!digits) return "";
  if (digits.startsWith("02")) {
    if (digits.length <= 2) return digits;
    if (digits.length <= 6) return digits.replace(/(\d{2})(\d{0,4})/, "$1-$2");
    return digits.replace(/(\d{2})(\d{4})(\d{0,4}).*/, "$1-$2-$3");
  }
  if (digits.length <= 3) return digits;
  if (digits.length <= 7) return digits.replace(/(\d{3})(\d{0,4})/, "$1-$2");
  return digits.replace(/(\d{3})(\d{3,4})(\d{0,4}).*/, "$1-$2-$3");
}

/** 백엔드 ApiResponse({ success, data, error })와 direct body를 모두 호환 */
function unwrapApi(data) {
  return data && typeof data === "object" && "data" in data ? data.data : data;
}

/** 에러 메시지 최대한 문자열로 정제하여 [object Object] 방지 */
function extractErrorMessage(err) {
  const server = err?.response?.data;
  let msg =
    (typeof server?.message === "string" && server.message) ||
    (typeof server?.error === "string" && server.error) ||
    (typeof server?.error?.message === "string" && server?.error?.message) ||
    (typeof server === "string" && server) ||
    err?.message ||
    "요청 처리 중 오류가 발생했습니다.";
  if (typeof msg !== "string") {
    try {
      msg = JSON.stringify(server);
    } catch {
      msg = "요청 처리 실패(원인 미상)";
    }
  }
  return msg;
}

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null); // { id, username, email, name, ... }
  const [token, setToken] = useState(null); // accessToken (JWT)
  const [loading, setLoading] = useState(true);

  // 초기 로드: 로컬스토리지에서 복원
  useEffect(() => {
    const savedToken = localStorage.getItem("accessToken");
    const savedUser = localStorage.getItem("user");
    
    if (savedToken) {
      setToken(savedToken);
    }
    if (savedUser) {
      try {
        const parsed = JSON.parse(savedUser);
        setUser(parsed);
      } catch (e) {
        console.error("❌ [AuthContext] Failed to parse user:", e);
      }
    }
    setLoading(false);
  }, []);

  /**
   * 로그인: 백엔드가 email + password를 기대하므로 이에 맞춤.
   * - Login.jsx에서 email, password로 호출하세요.
   * - 만약 UI가 한 칸(identifier)만 받는다면 identifier에 @가 있으면 email로 간주해 넘기세요.
   */
  const login = async ({ email, password, identifier } = {}) => {
    try {
      let finalEmail = (email ?? "").trim();
      if (!finalEmail && identifier) {
        finalEmail = identifier.includes("@") ? identifier.trim() : "";
      }
      if (!finalEmail) {
        throw new Error("이메일을 올바르게 입력해주세요.");
      }
      if (!password) {
        throw new Error("비밀번호를 입력해주세요.");
      }

      const payload = { email: finalEmail, password };
      const { data } = await authApi.login(payload);

      const body = unwrapApi(data);
      const accessToken = body?.accessToken || body?.token;
      const userInfo = body?.user || { email: finalEmail };

      if (!accessToken) {
        throw new Error("로그인 토큰이 응답에 없습니다.");
      }

      localStorage.setItem("accessToken", accessToken);
      localStorage.setItem("user", JSON.stringify(userInfo));
      setToken(accessToken);
      setUser(userInfo);
      return userInfo;
    } catch (err) {
      const msg = extractErrorMessage(err);
      const wrapped = new Error(msg);
      wrapped.response = err?.response;
      wrapped.cause = err;
      throw wrapped;
    }
  };

  // 회원가입: 이메일을 아이디로 사용 (username 자동 설정) + phone 자동 하이픈 적용
  const register = async (payload) => {
    try {
      // 기대 키: { email, name, password, phone?, birth? }
      // username은 백엔드에서 email로 자동 설정됨
      const clean = {
        email: payload?.email?.trim(),
        name: payload?.name?.trim(),
        password: payload?.password,
        ...(payload?.phone !== undefined && payload.phone !== ""
          ? { phone: formatPhoneKR(payload.phone) }
          : {}),
        ...(payload?.birth !== undefined && payload.birth !== null
          ? { birth: payload.birth }
          : {}),
      };

      const { data } = await authApi.signup(clean);

      return unwrapApi(data);
    } catch (err) {
      const msg = extractErrorMessage(err);
      const wrapped = new Error(msg);
      wrapped.response = err?.response;
      wrapped.cause = err;
      throw wrapped;
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
