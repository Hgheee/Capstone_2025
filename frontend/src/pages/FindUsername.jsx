import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { authApi } from "../lib/api";

export default function FindUsername() {
  const [form, setForm] = useState({ name: "", phoneOrEmail: "" });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      if (!form.name || !form.phoneOrEmail) {
        setError("모든 항목을 입력해주세요.");
        setLoading(false);
        return;
      }

      const response = await authApi.findUsername(form);
      if (response.data.success) {
        setResult(response.data.data);
      } else {
        setError(response.data.error?.message || "아이디 찾기에 실패했습니다.");
      }
    } catch (err) {
      const errorMessage =
        err?.response?.data?.error?.message ||
        err?.message ||
        "아이디 찾기 중 오류가 발생했습니다.";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-white font-elice">
      <div className="w-full max-w-[1100px] border border-gray-400 rounded-lg p-16 shadow-md bg-white">
        <h1 className="text-[56px] leading-[72px] text-[#3D3D3D] text-center mb-14">
          아이디 찾기
        </h1>
        <hr className="border-gray-300 mb-12" />

        {result ? (
          <div className="space-y-6 text-center">
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-8">
              <h2 className="text-2xl font-semibold text-gray-800 mb-4">
                아이디 찾기 완료
              </h2>
              <div className="space-y-3">
                <div>
                  <span className="text-gray-600">아이디: </span>
                  <span className="text-xl font-bold text-blue-600">
                    {result.username}
                  </span>
                </div>
                <div>
                  <span className="text-gray-600">이메일: </span>
                  <span className="text-lg text-gray-800">{result.email}</span>
                </div>
              </div>
            </div>
            <div className="flex gap-4 justify-center">
              <Link
                to="/login"
                className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                로그인하기
              </Link>
              <button
                onClick={() => {
                  setResult(null);
                  setForm({ name: "", phoneOrEmail: "" });
                }}
                className="px-6 py-3 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
              >
                다시 찾기
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={onSubmit} className="space-y-10 w-full max-w-[700px] mx-auto">
            <div>
              <label className="block text-[22px] text-[#3D3D3D] mb-3">
                이름
              </label>
              <input
                type="text"
                placeholder="이름을 입력하세요"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full h-[64px] border border-[#656565] rounded-[12px] px-5 text-lg"
                required
              />
            </div>

            <div>
              <label className="block text-[22px] text-[#3D3D3D] mb-3">
                전화번호 또는 이메일
              </label>
              <input
                type="text"
                placeholder="전화번호 또는 이메일을 입력하세요"
                value={form.phoneOrEmail}
                onChange={(e) =>
                  setForm({ ...form, phoneOrEmail: e.target.value })
                }
                className="w-full h-[64px] border border-[#656565] rounded-[12px] px-5 text-lg"
                required
              />
              <p className="mt-2 text-sm text-gray-500">
                전화번호 또는 이메일 중 하나를 입력해주세요.
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
                {loading ? "찾는 중..." : "아이디 찾기"}
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

