import { useState } from "react";
import { Link } from "react-router-dom";
import { authApi } from "../lib/api";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(false);

    try {
      if (!email || !email.includes("@")) {
        setError("올바른 이메일을 입력해주세요.");
        setLoading(false);
        return;
      }

      const response = await authApi.forgotPassword({ email });
      if (response.data.success) {
        setSuccess(true);
      } else {
        setError(response.data.error?.message || "비밀번호 찾기에 실패했습니다.");
      }
    } catch (err) {
      const errorMessage =
        err?.response?.data?.error?.message ||
        err?.message ||
        "비밀번호 찾기 중 오류가 발생했습니다.";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-white font-elice">
      <div className="w-full max-w-[1100px] border border-gray-400 rounded-lg p-16 shadow-md bg-white">
        <h1 className="text-[56px] leading-[72px] text-[#3D3D3D] text-center mb-14">
          비밀번호 찾기
        </h1>
        <hr className="border-gray-300 mb-12" />

        {success ? (
          <div className="space-y-6 text-center">
            <div className="bg-green-50 border border-green-200 rounded-lg p-8">
              <h2 className="text-2xl font-semibold text-gray-800 mb-4">
                임시 비밀번호 발급 완료
              </h2>
              <p className="text-lg text-gray-700 mb-4">
                입력하신 이메일로 임시 비밀번호가 발급되었습니다.
              </p>
              <p className="text-sm text-gray-500">
                개발 환경에서는 서버 로그를 확인하세요.
              </p>
            </div>
            <div className="flex gap-4 justify-center">
              <Link
                to="/login"
                className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                로그인하기
              </Link>
              <Link
                to="/reset-password"
                className="px-6 py-3 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
              >
                비밀번호 재설정
              </Link>
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
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full h-[64px] border border-[#656565] rounded-[12px] px-5 text-lg"
                required
              />
              <p className="mt-2 text-sm text-gray-500">
                등록하신 이메일 주소를 입력해주세요.
              </p>
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
                {loading ? "처리 중..." : "임시 비밀번호 발급"}
              </button>
            </div>

            <div className="text-center space-y-2">
              <Link
                to="/find-username"
                className="text-[18px] text-[#8B8B8B] hover:underline block"
              >
                아이디 찾기
              </Link>
              <Link
                to="/login"
                className="text-[18px] text-[#8B8B8B] hover:underline block"
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

