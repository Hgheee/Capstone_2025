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
    <header className="border-b">
      <div className="max-w-5xl mx-auto flex items-center justify-between p-4">
        <NavLink to="/home" className="text-xl font-semibold">
          Lost&Found
        </NavLink>
        <nav className="flex items-center gap-2">
          <NavLink to="/home" className={linkCls}>
            홈
          </NavLink>

          {user ? (
            <>
              <span className="text-sm text-gray-600">
                {user.name ? `${user.name}님` : user.email}
              </span>
              <button
                onClick={onLogout}
                className="px-3 py-2 rounded-md border hover:bg-gray-100"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={linkCls}>
                로그인
              </NavLink>
              <NavLink to="/signup" className={linkCls}>
                회원가입
              </NavLink>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
