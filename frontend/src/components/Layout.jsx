import { Outlet } from "react-router-dom";
import Header from "./Header";

export default function Layout() {
  return (
    <div className="min-h-screen flex flex-col font-elice bg-gray-50">
      {/* ✅ Header 컴포넌트 사용 (AuthContext 기반) */}
      <Header />

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
