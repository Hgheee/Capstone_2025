import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

export default function Login() {
  const [form, setForm] = useState({ username: "", password: "" });
  const { login } = useAuth();
  const navigate = useNavigate();
  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      await login(form);
      navigate("/home");
    } catch (err) {
      // 백엔드 에러 메시지 표시
      const msg =
        err.response?.data?.message || "로그인 중 오류가 발생했습니다.";
      alert(msg);
      console.error(err);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-white font-elice">
      {/* 전체 컨테이너*/}
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
          {/* 아이디 */}
          <div>
            <label className="block text-[22px] text-[#3D3D3D] mb-3">
              아이디
            </label>
            <input
              type="text"
              placeholder="아이디 입력"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              className="w-full h-[64px] border border-[#656565] rounded-[12px] px-5 text-lg"
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
            />
          </div>

          {/* 비밀번호 찾기 */}
          <div className="text-right">
            <button
              type="button"
              className="text-[18px] text-[#8B8B8B] hover:underline"
            >
              비밀번호를 잊으셨나요?
            </button>
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
