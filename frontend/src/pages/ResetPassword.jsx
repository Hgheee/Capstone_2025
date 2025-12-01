import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { authApi } from "../lib/api";

export default function ResetPassword() {
  const [form, setForm] = useState({
    email: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(false);

    try {
      if (!form.email || !form.email.includes("@")) {
        setError("올바른 이메일을 입력해주세요.");
        setLoading(false);
        return;
      }

      if (!form.newPassword || form.newPassword.length < 6) {
        setError("비밀번호는 최소 6자 이상이어야 합니다.");
        setLoading(false);
        return;
      }

      if (form.newPassword !== form.confirmPassword) {
        setError("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        setLoading(false);
        return;
      }

      const response = await authApi.resetPassword(form);
      if (response.data.success) {
        setSuccess(true);
        setTimeout(() => {
          navigate("/login");
        }, 2000);
      } else {
        setError(response.data.error?.message || "비밀번호 재설정에 실패했습니다.");
      }
    } catch (err) {
      const errorMessage =
        err?.response?.data?.error?.message ||
        err?.message ||
        "비밀번호 재설정 중 오류가 발생했습니다.";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-white font-elice">
      <div className="w-full max-w-[1100px] border border-gray-400 rounded-lg p-16 shadow-md bg-white">
        <h1 className="text-[56px] leading-[72px] text-[#3D3D3D] text-center mb-14">
          비밀번호 재설정
        </h1>
        <hr className="border-gray-300 mb-12" />

        {success ? (
          <div className="space-y-6 text-center">
            <div className="bg-green-50 border border-green-200 rounded-lg p-8">
              <h2 className="text-2xl font-semibold text-gray-800 mb-4">
                비밀번호 재설정 완료
              </h2>
              <p className="text-lg text-gray-700">
                비밀번호가 성공적으로 재설정되었습니다.
              </p>
              <p className="text-sm text-gray-500 mt-2">
                잠시 후 로그인 페이지로 이동합니다.
              </p>
            </div>
          </div>
        ) : (
          <form onSubmit={onSubmit} className="space-y-10 w-full max-w-[700px] mx-auto">
            <div>
              <label className="block text-[22px] text-[#3D3D3D] mb-3">
                이메일
              </label>
              <input
                type="email"
                placeholder="이메일을 입력하세요"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                className="w-full h-[64px] border border-[#656565] rounded-[12px] px-5 text-lg"
                required
              />
            </div>

            <div>
              <label className="block text-[22px] text-[#3D3D3D] mb-3">
                새 비밀번호
              </label>
              <input
                type="password"
                placeholder="새 비밀번호를 입력하세요 (최소 6자)"
                value={form.newPassword}
                onChange={(e) =>
                  setForm({ ...form, newPassword: e.target.value })
                }
                className="w-full h-[64px] border border-[#656565] rounded-[12px] px-5 text-lg"
                required
                minLength={6}
              />
            </div>

            <div>
              <label className="block text-[22px] text-[#3D3D3D] mb-3">
                비밀번호 확인
              </label>
              <input
                type="password"
                placeholder="비밀번호를 다시 입력하세요"
                value={form.confirmPassword}
                onChange={(e) =>
                  setForm({ ...form, confirmPassword: e.target.value })
                }
                className="w-full h-[64px] border border-[#656565] rounded-[12px] px-5 text-lg"
                required
                minLength={6}
              />
            </div>

            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
                {error}
              </div>
            )}

            <div className="flex justify-center pt-8">
              <button
                type="submit"
                disabled={loading}
                className="w-full max-w-[400px] h-[60px] rounded-[90px] text-white text-[22px] disabled:opacity-50 disabled:cursor-not-allowed"
                style={{
                  background:
                    "linear-gradient(0deg, rgba(0,0,0,0.2), rgba(0,0,0,0.2)), #A2AADB",
                }}
              >
                {loading ? "처리 중..." : "비밀번호 재설정"}
              </button>
            </div>

            <div className="text-center">
              <Link
                to="/login"
                className="text-[18px] text-[#8B8B8B] hover:underline"
              >
                로그인으로 돌아가기
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

