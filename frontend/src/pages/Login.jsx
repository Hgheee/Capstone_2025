import { useState } from "react";

export default function Login() {
  const [form, setForm] = useState({
    username: "",
    password: "",
  });

  const onSubmit = (e) => {
    e.preventDefault();
    console.log("로그인 시도:", form);
    // TODO: fetch("http://localhost:8081/api/auth/login", {...})
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-white font-elice">
      {/* 전체 컨테이너 */}
      <div className="w-full max-w-[600px] border border-gray-400 rounded-lg p-10 shadow-sm bg-white">
        {/* 제목 */}
        <h1 className="text-[48px] leading-[72px] text-[#3D3D3D] text-center mb-10">
          로그인
        </h1>
        <hr className="border-gray-300 mb-8" />

        {/* 폼 */}
        <form onSubmit={onSubmit} className="space-y-6 w-full">
          {/* 아이디 */}
          <div>
            <label className="block text-[20px] text-[#3D3D3D] mb-2">
              아이디
            </label>
            <input
              type="text"
              placeholder="아이디 입력"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              className="w-full h-[56px] border border-[#656565] rounded-[12px] px-4 text-base"
            />
          </div>

          {/* 비밀번호 */}
          <div>
            <label className="block text-[20px] text-[#3D3D3D] mb-2">
              비밀번호
            </label>
            <input
              type="password"
              placeholder="비밀번호 입력"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              className="w-full h-[56px] border border-[#848484] rounded-[12px] px-4 text-base"
            />
          </div>

          {/* 비밀번호 찾기 */}
          <div className="text-right">
            <button
              type="button"
              className="text-[16px] text-[#8B8B8B] hover:underline"
            >
              비밀번호를 잊으셨나요?
            </button>
          </div>

          {/* 로그인 버튼 */}
          <div className="flex justify-center pt-6">
            <button
              type="submit"
              className="w-full max-w-[300px] h-[50px] rounded-[90px] text-white text-[20px]"
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
