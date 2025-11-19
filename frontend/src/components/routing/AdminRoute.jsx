import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";

/**
 * 관리자 권한이 있는 사용자만 접근 가능한 라우트
 * user.role이 'ADMIN'인 경우에만 자식 컴포넌트를 렌더링
 */
export default function AdminRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-gray-500">로딩 중...</div>
      </div>
    );
  }

  // 로그인하지 않은 경우 로그인 페이지로 리다이렉트
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // 관리자가 아닌 경우 홈으로 리다이렉트
  if (user.role !== "ADMIN") {
    alert("관리자 권한이 필요합니다.");
    return <Navigate to="/home" replace />;
  }

  // 관리자인 경우 자식 컴포넌트 렌더링
  return <Outlet />;
}