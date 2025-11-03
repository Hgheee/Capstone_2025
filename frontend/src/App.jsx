import { Routes, Route, Navigate } from "react-router-dom";

//공용 레이아웃 (Header/Footer 포함)
import Layout from "./components/Layout.jsx";

//페이지 컴포넌트
import Home from "./pages/Home.jsx";
import Search from "./pages/Search.jsx";
import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx";
import NotFound from "./pages/NotFound.jsx";

import ProtectedRoute from "./components/routing/ProtectedRoute.jsx";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        {/* 기본 경로 → /home으로 리다이렉트 */}
        <Route path="/" element={<Navigate to="/home" replace />} />

        {/* 페이지 라우트 */}
        <Route path="/home" element={<Home />} />
        <Route path="/search" element={<Search />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/*보호 라우트 */}
        <Route element={<ProtectedRoute />}>
          {/* <Route path="/dashboard" element={<Dashboard />} /> */}
        </Route>

        {/* 404 페이지 */}
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
