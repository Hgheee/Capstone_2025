import { useState, useEffect } from "react";
import { useAuth } from "../contexts/AuthContext";
import { api } from "../lib/api";

export default function MyPage() {
  const { user, logout } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [form, setForm] = useState({
    name: "",
    phone: "",
  });
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  useEffect(() => {
    if (user) {
      setForm({
        name: user.name || "",
        phone: user.phone || "",
      });
    }
  }, [user]);

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    
    if (!form.name.trim()) {
      alert("이름을 입력해주세요.");
      return;
    }

    try {
      const { data } = await api.put("/auth/me", form);
      alert("프로필이 수정되었습니다.");
      setIsEditing(false);
      // 로컬 스토리지 업데이트
      if (data?.data) {
        localStorage.setItem("user", JSON.stringify(data.data));
        window.location.reload(); // 간단하게 새로고침
      }
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        "프로필 수정 중 오류가 발생했습니다.";
      alert(msg);
      console.error(err);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();

    if (!passwordForm.currentPassword) {
      alert("현재 비밀번호를 입력해주세요.");
      return;
    }

    if (passwordForm.newPassword.length < 8) {
      alert("새 비밀번호는 8자 이상이어야 합니다.");
      return;
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      alert("새 비밀번호가 일치하지 않습니다.");
      return;
    }

    try {
      await api.put("/auth/change-password", {
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
        confirmPassword: passwordForm.confirmPassword,
      });
      alert("비밀번호가 변경되었습니다. 다시 로그인해주세요.");
      logout();
      window.location.href = "/login";
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        "비밀번호 변경 중 오류가 발생했습니다.";
      alert(msg);
      console.error(err);
    }
  };

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-gray-500">로그인이 필요합니다.</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-12 font-elice">
      <div className="max-w-4xl mx-auto px-4">
        {/* 헤더 */}
        <div className="bg-white rounded-lg shadow-sm p-8 mb-6">
          <h1 className="text-3xl font-bold text-[#3D3D3D] mb-2">마이페이지</h1>
          <p className="text-gray-600">회원 정보를 관리하세요</p>
        </div>

        {/* 기본 정보 */}
        <div className="bg-white rounded-lg shadow-sm p-8 mb-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-semibold text-[#3D3D3D]">기본 정보</h2>
            {!isEditing && (
              <button
                onClick={() => setIsEditing(true)}
                className="px-4 py-2 bg-[#A2AADB] text-white rounded-md hover:bg-[#8B94C7] transition-colors"
              >
                수정하기
              </button>
            )}
          </div>

          <form onSubmit={handleUpdateProfile}>
            <div className="space-y-4">
              {/* 이메일 (읽기 전용) */}
              <div>
                <label className="block text-lg font-medium text-gray-700 mb-2">
                  이메일 (아이디)
                </label>
                <input
                  type="email"
                  value={user.email}
                  disabled
                  className="w-full h-12 border border-gray-300 rounded-lg px-4 bg-gray-100 text-gray-600 cursor-not-allowed"
                />
              </div>

              {/* 이름 */}
              <div>
                <label className="block text-lg font-medium text-gray-700 mb-2">
                  이름
                </label>
                <input
                  type="text"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  disabled={!isEditing}
                  className={`w-full h-12 border rounded-lg px-4 ${
                    isEditing
                      ? "border-gray-400 bg-white"
                      : "border-gray-300 bg-gray-50 text-gray-600"
                  }`}
                />
              </div>

              {/* 전화번호 */}
              <div>
                <label className="block text-lg font-medium text-gray-700 mb-2">
                  전화번호
                </label>
                <input
                  type="tel"
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                  disabled={!isEditing}
                  placeholder="010-1234-5678"
                  className={`w-full h-12 border rounded-lg px-4 ${
                    isEditing
                      ? "border-gray-400 bg-white"
                      : "border-gray-300 bg-gray-50 text-gray-600"
                  }`}
                />
              </div>

              {/* 가입일 */}
              <div>
                <label className="block text-lg font-medium text-gray-700 mb-2">
                  가입일
                </label>
                <input
                  type="text"
                  value={
                    user.createdAt
                      ? new Date(user.createdAt).toLocaleDateString("ko-KR")
                      : "정보 없음"
                  }
                  disabled
                  className="w-full h-12 border border-gray-300 rounded-lg px-4 bg-gray-100 text-gray-600 cursor-not-allowed"
                />
              </div>
            </div>

            {isEditing && (
              <div className="flex gap-3 mt-6">
                <button
                  type="submit"
                  className="flex-1 h-12 bg-[#A2AADB] text-white rounded-lg hover:bg-[#8B94C7] transition-colors font-medium"
                >
                  저장하기
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsEditing(false);
                    setForm({
                      name: user.name || "",
                      phone: user.phone || "",
                    });
                  }}
                  className="flex-1 h-12 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition-colors font-medium"
                >
                  취소
                </button>
              </div>
            )}
          </form>
        </div>

        {/* 비밀번호 변경 */}
        <div className="bg-white rounded-lg shadow-sm p-8 mb-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-semibold text-[#3D3D3D]">
              비밀번호 변경
            </h2>
            {!isChangingPassword && (
              <button
                onClick={() => setIsChangingPassword(true)}
                className="px-4 py-2 bg-[#A2AADB] text-white rounded-md hover:bg-[#8B94C7] transition-colors"
              >
                비밀번호 변경
              </button>
            )}
          </div>

          {isChangingPassword && (
            <form onSubmit={handleChangePassword}>
              <div className="space-y-4">
                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-2">
                    현재 비밀번호
                  </label>
                  <input
                    type="password"
                    value={passwordForm.currentPassword}
                    onChange={(e) =>
                      setPasswordForm({
                        ...passwordForm,
                        currentPassword: e.target.value,
                      })
                    }
                    className="w-full h-12 border border-gray-400 rounded-lg px-4"
                    autoComplete="current-password"
                  />
                </div>

                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-2">
                    새 비밀번호
                  </label>
                  <input
                    type="password"
                    value={passwordForm.newPassword}
                    onChange={(e) =>
                      setPasswordForm({
                        ...passwordForm,
                        newPassword: e.target.value,
                      })
                    }
                    placeholder="8자 이상 입력"
                    className="w-full h-12 border border-gray-400 rounded-lg px-4"
                    autoComplete="new-password"
                  />
                </div>

                <div>
                  <label className="block text-lg font-medium text-gray-700 mb-2">
                    새 비밀번호 확인
                  </label>
                  <input
                    type="password"
                    value={passwordForm.confirmPassword}
                    onChange={(e) =>
                      setPasswordForm({
                        ...passwordForm,
                        confirmPassword: e.target.value,
                      })
                    }
                    placeholder="새 비밀번호 재입력"
                    className="w-full h-12 border border-gray-400 rounded-lg px-4"
                    autoComplete="new-password"
                  />
                </div>
              </div>

              <div className="flex gap-3 mt-6">
                <button
                  type="submit"
                  className="flex-1 h-12 bg-[#A2AADB] text-white rounded-lg hover:bg-[#8B94C7] transition-colors font-medium"
                >
                  변경하기
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsChangingPassword(false);
                    setPasswordForm({
                      currentPassword: "",
                      newPassword: "",
                      confirmPassword: "",
                    });
                  }}
                  className="flex-1 h-12 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition-colors font-medium"
                >
                  취소
                </button>
              </div>
            </form>
          )}
        </div>

        {/* 회원 탈퇴 (선택사항) */}
        <div className="bg-white rounded-lg shadow-sm p-8 border-2 border-red-200">
          <h2 className="text-xl font-semibold text-red-600 mb-3">회원 탈퇴</h2>
          <p className="text-gray-600 mb-4">
            회원 탈퇴 시 모든 정보가 삭제되며 복구할 수 없습니다.
          </p>
          <button
            onClick={() => {
              if (
                window.confirm(
                  "정말로 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다."
                )
              ) {
                alert("회원 탈퇴 기능은 준비 중입니다.");
              }
            }}
            className="px-6 py-2 bg-red-500 text-white rounded-md hover:bg-red-600 transition-colors font-medium"
          >
            회원 탈퇴
          </button>
        </div>
      </div>
    </div>
  );
}




