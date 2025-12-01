import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

const linkCls = ({ isActive }) =>
  `px-3 py-2 rounded-md ${isActive ? "bg-gray-200" : "hover:bg-gray-100"}`;

export default function Header() {
  const { user, logout } = useAuth();
  const nav = useNavigate();

  const onLogout = () => {
    logout();
    nav("/login");
  };

  return (
    <header className="border-b bg-white shadow-sm">
      <div className="max-w-6xl mx-auto flex items-center justify-between px-6 py-4">
        <NavLink to="/home" className="text-2xl font-bold text-[#A2AADB] hover:text-[#8B94C7] transition-colors">
          Lost&Found
        </NavLink>
        <nav className="flex items-center gap-4">
          <NavLink to="/home" className={linkCls}>
            홈
          </NavLink>

          {user ? (
            <>
              <NavLink to="/search" className={linkCls}>
                검색
              </NavLink>
              <NavLink to="/mypage" className={linkCls}>
                마이페이지
              </NavLink>
              {user.role === "ADMIN" && (
                <NavLink to="/admin" className={linkCls}>
                  <span className="text-purple-600 font-semibold">관리자</span>
                </NavLink>
              )}
              <span className="text-sm text-gray-600 px-2">
                {user.name ? `${user.name}님` : user.email}
                {user.role === "ADMIN" && (
                  <span className="ml-1 text-xs text-purple-600 font-semibold">(관리자)</span>
                )}
              </span>
              <button
                onClick={onLogout}
                className="px-4 py-2 rounded-md bg-[#A2AADB] text-white hover:bg-[#8B94C7] transition-colors font-medium"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={linkCls}>
                로그인
              </NavLink>
              <NavLink to="/register" className={linkCls}>
                회원가입
              </NavLink>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
