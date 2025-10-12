import { Outlet, Link } from "react-router-dom";

export default function Layout() {
  return (
    <div className="min-h-screen flex flex-col font-elice bg-gray-50">
      {/* 헤더 */}
      <header className="border-b bg-white shadow-sm">
        <div className="max-w-[1100px] mx-auto flex justify-between items-center p-4">
          {/* 로고 */}
          <Link to="/home" className="text-2xl font-bold text-[#3D3D3D]">
            Lost & Found
          </Link>

          {/* 네비게이션 */}
          <nav className="flex gap-6 text-[#3D3D3D] text-lg">
            <Link to="/home" className="hover:text-[#6E6E6E]">
              홈
            </Link>
            <Link to="/login" className="hover:text-[#6E6E6E]">
              로그인
            </Link>
            <Link to="/register" className="hover:text-[#6E6E6E]">
              회원가입
            </Link>
          </nav>
        </div>
      </header>

      {/* 메인 컨텐츠 */}
      <main className="flex-1 flex justify-center items-center p-6">
        <Outlet />
      </main>

      {/* 푸터 */}
      <footer className="border-t bg-white text-center text-sm text-gray-500 py-4">
        2025 Capstone design project - Lost & Found.
      </footer>
    </div>
  );
}
