import { createContext, useContext, useEffect, useState } from "react";
import { setAuthToken } from "../lib/api";

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null); // { email, name?, token? }

  // 앱 시작 시 localStorage에서 복원
  useEffect(() => {
    try {
      const saved = JSON.parse(localStorage.getItem("auth:user"));
      if (saved) {
        setUser(saved);
        if (saved.token) setAuthToken(saved.token);
      }
    } catch {}
  }, []);

  const login = (u) => {
    setUser(u);
    localStorage.setItem("auth:user", JSON.stringify(u));
    if (u?.token) setAuthToken(u.token);
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem("auth:user");
    setAuthToken(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
