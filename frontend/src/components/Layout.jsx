import { Outlet } from "react-router-dom";
import Header from "./Header.jsx";

export default function Layout() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <main className="flex-1 max-w-5xl mx-auto p-4">
        <Outlet />
      </main>
      <footer className="border-t p-4 text-center text-sm text-gray-500">
        © 2025 Lost&Found
      </footer>
    </div>
  );
}
