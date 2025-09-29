import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

export default function Register() {
  const [form, setForm] = useState({
    username: "",
    password: "",
    confirmPassword: "",
    name: "",
    phone: "",
    emailUser: "",
    emailDomain: "",
    year: "",
    month: "",
    day: "",
  });
  const { register } = useAuth(); // ✅ 컨텍스트 사용
  const navigate = useNavigate();

  const onSubmit = async (e) => {
    e.preventDefault();

    if (form.password !== form.confirmPassword) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    // 이메일/생년월일 합치기 (백엔드가 원하는 형태에 맞추세요)
    const payload = {
      username: form.username,
      password: form.password,
      name: form.name,
      phone: form.phone,
      email: `${form.emailUser}@${form.emailDomain}`,
      birth: `${form.year}-${form.month}-${form.day}`, // "YYYY-MM-DD"
    };

    try {
      await register(payload);
      alert("회원가입이 완료되었습니다. 로그인 해주세요.");
      navigate("/login");
    } catch (err) {
      const msg =
        err.response?.data?.message || "회원가입 중 오류가 발생했습니다.";
      alert(msg);
      console.error(err);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-white font-elice">
      {/* 전체 컨테이너 */}
      <div className="w-full max-w-[1100px] border border-gray-400 rounded-lg p-10 shadow-sm bg-white">
        {/* 제목 */}
        <h1 className="text-[48px] leading-[72px] text-[#3D3D3D] text-center mb-10">
          회원가입
        </h1>
        <hr className="border-gray-300 mb-8" />

        {/* 폼 */}
        <form
          onSubmit={onSubmit}
          className="space-y-6 w-full max-w-[700px] mx-auto"
        >
          {/* 아이디 + 중복확인 */}
          <div>
            <label className="block text-[20px] text-[#3D3D3D] mb-2">
              아이디
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="아이디 입력 (6~20자)"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                className="flex-grow h-[56px] border border-[#656565] rounded-[12px] px-4 text-base"
              />
              <button
                type="button"
                className="px-4 h-[56px] rounded-[10px] bg-[#A2AADB] text-white text-[16px] whitespace-nowrap"
                style={{
                  background:
                    "linear-gradient(0deg, rgba(0,0,0,0.2), rgba(0,0,0,0.2)), #A2AADB",
                }}
              >
                중복확인
              </button>
            </div>
          </div>

          {/* 비밀번호 */}
          <div>
            <label className="block text-[20px] text-[#3D3D3D] mb-2">
              비밀번호
            </label>
            <input
              type="password"
              placeholder="비밀번호 입력 (문자, 숫자, 특수문자 포함 8~20자)"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              className="w-full h-[56px] border border-[#848484] rounded-[12px] px-4 text-base"
            />
          </div>

          {/* 비밀번호 확인 */}
          <div>
            <label className="block text-[20px] text-[#3D3D3D] mb-2">
              비밀번호 확인
            </label>
            <input
              type="password"
              placeholder="비밀번호 재입력"
              value={form.confirmPassword}
              onChange={(e) =>
                setForm({ ...form, confirmPassword: e.target.value })
              }
              className="w-full h-[56px] border border-[#606060] rounded-[12px] px-4 text-base"
            />
          </div>

          {/* 이름 */}
          <div>
            <label className="block text-[20px] text-[#3D3D3D] mb-2">
              이름
            </label>
            <input
              type="text"
              placeholder="이름을 입력해주세요"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="w-full h-[56px] border border-[#4E4E4E] rounded-[12px] px-4 text-base"
            />
          </div>

          {/* 전화번호 */}
          <div>
            <label className="block text-[20px] text-[#3D3D3D] mb-2">
              전화번호
            </label>
            <input
              type="text"
              placeholder="휴대폰 번호 입력 (‘-’ 제외 11자리 입력)"
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
              className="w-full h-[56px] border border-[#6E6E6E] rounded-[12px] px-4 text-base"
            />
          </div>

          {/* 이메일 */}
          <div>
            <label className="block text-[20px] text-[#3D3D3D] mb-2">
              이메일 주소
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="이메일 아이디"
                value={form.emailUser}
                onChange={(e) =>
                  setForm({ ...form, emailUser: e.target.value })
                }
                className="flex-1 h-[56px] border border-[#4D4D4D] rounded-[12px] px-4 text-base"
              />
              <span className="text-[20px]">@</span>
              <input
                type="text"
                placeholder="직접 입력"
                value={form.emailDomain}
                onChange={(e) =>
                  setForm({ ...form, emailDomain: e.target.value })
                }
                className="flex-1 h-[56px] border border-[#4D4D4D] rounded-[12px] px-4 text-base"
              />
            </div>
          </div>

          {/* 생년월일 */}
          <div>
            <label className="block text-[20px] text-[#3D3D3D] mb-2">
              생년월일
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="년도"
                value={form.year}
                onChange={(e) => setForm({ ...form, year: e.target.value })}
                className="flex-1 h-[56px] border border-[#6E6E6E] rounded-[12px] px-4 text-base"
              />
              <input
                type="text"
                placeholder="월"
                value={form.month}
                onChange={(e) => setForm({ ...form, month: e.target.value })}
                className="flex-1 h-[56px] border border-[#6E6E6E] rounded-[12px] px-4 text-base"
              />
              <input
                type="text"
                placeholder="일"
                value={form.day}
                onChange={(e) => setForm({ ...form, day: e.target.value })}
                className="flex-1 h-[56px] border border-[#6E6E6E] rounded-[12px] px-4 text-base"
              />
            </div>
          </div>

          {/* 가입하기 버튼 */}
          <div className="flex justify-center pt-6">
            <button
              type="submit"
              className="w-full max-w-[300px] h-[50px] rounded-[90px] text-white text-[20px]"
              style={{
                background:
                  "linear-gradient(0deg, rgba(0,0,0,0.2), rgba(0,0,0,0.2)), #A2AADB",
              }}
            >
              가입하기
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
