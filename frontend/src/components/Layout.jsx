import { Outlet } from "react-router-dom";
import Header from "./Header.jsx";

export default function Layout() {
  return (
    <div className="min-h-screen bg-white">
      {/* Header 자리 */}
      <header className="w-full h-[60px] bg-gray-200 flex items-center px-4">
        <h1 className="font-bold text-lg">Lost & Found</h1>
      </header>

      {/* 본문 */}
      <main className="container mx-auto p-4">
        <Outlet />
      </main>

      {/* Footer 자리 */}
      <footer className="w-full h-[50px] bg-gray-100 flex items-center justify-center">
        <p className="text-sm text-gray-500">© 2025 Lost & Found</p>
      </footer>
    </div>
  );
}
