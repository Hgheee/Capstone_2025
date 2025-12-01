import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

export default function Login() {
  // ✅ 백엔드가 email + password를 기대하므로 상태를 이메일 기반으로 변경
  const [form, setForm] = useState({ email: "", password: "" });
  const { login } = useAuth();
  const navigate = useNavigate();

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      // 최소 검증: 이메일 형식
      if (!form.email || !form.email.includes("@")) {
        alert("이메일을 올바르게 입력해주세요.");
        return;
      }
      if (!form.password) {
        alert("비밀번호를 입력해주세요.");
        return;
      }

      // ✅ AuthContext.login은 { email, password } 를 받도록 수정됨
      await login({ email: form.email, password: form.password });
      
      // ✅ 로그인 후 강제 새로고침 (AuthContext 상태 확실히 업데이트)
      window.location.href = "/home";
    } catch (err) {
      // ✅ AuthContext에서 이미 사람이 읽을 수 있는 message로 변환해 던지므로 우선 사용
      const msg =
        err?.message ||
        err?.response?.data?.message ||
        "로그인 중 오류가 발생했습니다.";
      alert(msg);
      console.error("LOGIN FAIL ::", {
        status: err?.response?.status,
        data: err?.response?.data,
        message: err?.message,
      });
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-white font-elice">
      {/* 전체 컨테이너 */}
      <div className="w-full max-w-[1100px] border border-gray-400 rounded-lg p-16 shadow-md bg-white">
        {/* 제목 */}
        <h1 className="text-[56px] leading-[72px] text-[#3D3D3D] text-center mb-14">
          로그인
        </h1>
        <hr className="border-gray-300 mb-12" />

        {/* 폼 */}
        <form
          onSubmit={onSubmit}
          className="space-y-10 w-full max-w-[700px] mx-auto"
        >
          {/* 이메일 */}
          <div>
            <label className="block text-[22px] text-[#3D3D3D] mb-3">
              이메일
            </label>
            <input
              type="email"
              placeholder="이메일 입력"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="w-full h-[64px] border border-[#656565] rounded-[12px] px-5 text-lg"
              autoComplete="username"
              inputMode="email"
            />
          </div>

          {/* 비밀번호 */}
          <div>
            <label className="block text-[22px] text-[#3D3D3D] mb-3">
              비밀번호
            </label>
            <input
              type="password"
              placeholder="비밀번호 입력"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              className="w-full h-[64px] border border-[#848484] rounded-[12px] px-5 text-lg"
              autoComplete="current-password"
            />
          </div>

          {/* 아이디/비밀번호 찾기 */}
          <div className="flex justify-center gap-4 pt-4">
            <Link
              to="/find-username"
              className="px-6 py-3 text-[18px] text-[#8B8B8B] border border-gray-300 rounded-lg hover:bg-gray-50 hover:border-gray-400 transition-colors"
            >
              아이디 찾기
            </Link>
            <Link
              to="/forgot-password"
              className="px-6 py-3 text-[18px] text-[#8B8B8B] border border-gray-300 rounded-lg hover:bg-gray-50 hover:border-gray-400 transition-colors"
            >
              비밀번호 찾기
            </Link>
          </div>

          {/* 로그인 버튼 */}
          <div className="flex justify-center pt-8">
            <button
              type="submit"
              className="w-full max-w-[400px] h-[60px] rounded-[90px] text-white text-[22px]"
              style={{
                background:
                  "linear-gradient(0deg, rgba(0,0,0,0.2), rgba(0,0,0,0.2)), #A2AADB",
              }}
            >
              로그인
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
