import { NavLink } from "react-router-dom";

const link = ({ isActive }) =>
  `px-3 py-2 rounded-md ${isActive ? "bg-gray-200" : "hover:bg-gray-100"}`;

export default function Header() {
  return (
    <header className="border-b">
      <div className="max-w-5xl mx-auto flex items-center justify-between p-4">
        <NavLink to="/home" className="text-xl font-semibold">
          Lost&Found
        </NavLink>
        <nav className="flex gap-2">
          <NavLink to="/home" className={link}>
            홈
          </NavLink>
          <NavLink to="/login" className={link}>
            로그인
          </NavLink>
          <NavLink to="/signup" className={link}>
            회원가입
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
