import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

export default function Register() {
  const [form, setForm] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    name: "",
    phone: "", // ✅ '' 초기화 유지
    year: "",
    month: "",
    day: "",
  });

  const { register } = useAuth();
  const navigate = useNavigate();

  /** 휴대폰 번호 하이픈 포맷터 (blur 때만 적용 권장) */
  function formatPhoneKR(input) {
    const digits = (input ?? "").replace(/\D/g, ""); // 숫자만
    if (!digits) return "";
    if (digits.startsWith("02")) {
      // 02-XXXX-XXXX
      if (digits.length <= 2) return digits;
      if (digits.length <= 6)
        return digits.replace(/(\d{2})(\d{0,4})/, "$1-$2");
      return digits.replace(/(\d{2})(\d{4})(\d{0,4}).*/, "$1-$2-$3");
    }
    // 010/011/016/017/018/019
    if (digits.length <= 3) return digits;
    if (digits.length <= 7) return digits.replace(/(\d{3})(\d{0,4})/, "$1-$2");
    return digits.replace(/(\d{3})(\d{3,4})(\d{0,4}).*/, "$1-$2-$3");
  }

  /** 공백 제거 + 기본 검증 */
  function validate() {
    if (!form.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()))
      return "유효한 이메일을 입력하세요.";
    if (!form.password || form.password.length < 8)
      return "비밀번호는 8자 이상 입력하세요.";
    if (form.password !== form.confirmPassword)
      return "비밀번호가 일치하지 않습니다.";
    if (!form.name.trim()) return "이름을 입력하세요.";
    return null;
  }

  /** 숫자 2자리/월일 zero-pad */
  const pad2 = (v) => String(v ?? "").padStart(2, "0");

  const onSubmit = async (e) => {
    e.preventDefault();
    const msg = validate();
    if (msg) return alert(msg);

    // YYYY-MM-DD (월/일 zero-pad)
    const birth = form.year && form.month && form.day 
      ? `${form.year}-${pad2(form.month)}-${pad2(form.day)}`
      : null;

    // blur에서 이미 포맷되었더라도 한 번 더 안전하게 보정하고 전송
    const payload = {
      email: form.email.trim(),
      password: form.password,
      name: form.name.trim(),
      phone: formatPhoneKR(form.phone), // ✅ 하이픈 포함으로 백엔드 정규식 통과
      birth,
    };

    try {
      await register(payload);
      alert("회원가입이 완료되었습니다. 로그인 해주세요.");
      window.location.href = "/login";
    } catch (err) {
      const msg =
        err?.message ||
        err?.response?.data?.error?.message ||
        "회원가입 중 오류가 발생했습니다.";
      alert(msg);
      console.error(err);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-white font-elice">
      <div className="w-full max-w-[1100px] border border-gray-400 rounded-lg p-10 shadow-sm bg-white">
        <h1 className="text-[48px] leading-[72px] text-[#3D3D3D] text-center mb-10">
          회원가입
        </h1>
        <hr className="border-gray-300 mb-8" />

        <form
          onSubmit={onSubmit}
          className="space-y-6 w-full max-w-[700px] mx-auto"
        >
          {/* 이메일 */}
          <div>
            <label className="block text-[20px] mb-2">이메일 (아이디)</label>
            <input
              type="email"
              placeholder="example@email.com"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="w-full h-[56px] border border-[#656565] rounded-[12px] px-4 text-base"
              autoComplete="email"
            />
          </div>

          {/* 비밀번호 */}
          <div>
            <label className="block text-[20px] mb-2">비밀번호</label>
            <input
              type="password"
              placeholder="문자, 숫자, 특수문자 포함 8~20자"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              className="w-full h-[56px] border border-[#848484] rounded-[12px] px-4 text-base"
              autoComplete="new-password"
            />
          </div>

          {/* 비밀번호 확인 */}
          <div>
            <label className="block text-[20px] mb-2">비밀번호 확인</label>
            <input
              type="password"
              placeholder="비밀번호 재입력"
              value={form.confirmPassword}
              onChange={(e) =>
                setForm({ ...form, confirmPassword: e.target.value })
              }
              className="w-full h-[56px] border border-[#606060] rounded-[12px] px-4 text-base"
              autoComplete="new-password"
            />
          </div>

          {/* 이름 */}
          <div>
            <label className="block text-[20px] mb-2">이름</label>
            <input
              type="text"
              placeholder="이름을 입력해주세요"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="w-full h-[56px] border border-[#4E4E4E] rounded-[12px] px-4 text-base"
              autoComplete="name"
            />
          </div>

          {/* 전화번호 (선택사항) */}
          <div>
            <label className="block text-[20px] mb-2">전화번호 (선택사항)</label>
            <input
              type="tel"
              inputMode="numeric"
              placeholder="010-1234-5678"
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
              onBlur={() =>
                setForm((prev) => ({
                  ...prev,
                  phone: formatPhoneKR(prev.phone),
                }))
              } // blur 시 포맷
              maxLength={13}
              className="w-full h-[56px] border border-[#6E6E6E] rounded-[12px] px-4 text-base"
              autoComplete="tel"
            />
          </div>

          {/* 생년월일 (선택사항) */}
          <div>
            <label className="block text-[20px] mb-2">생년월일 (선택사항)</label>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="년도"
                value={form.year}
                onChange={(e) =>
                  setForm({ ...form, year: e.target.value.replace(/\D/g, "") })
                }
                className="flex-1 h-[56px] border border-[#606060] rounded-[12px] px-4"
                inputMode="numeric"
                maxLength={4}
              />
              <input
                type="text"
                placeholder="월"
                value={form.month}
                onChange={(e) =>
                  setForm({ ...form, month: e.target.value.replace(/\D/g, "") })
                }
                className="flex-1 h-[56px] border border-[#606060] rounded-[12px] px-4"
                inputMode="numeric"
                maxLength={2}
              />
              <input
                type="text"
                placeholder="일"
                value={form.day}
                onChange={(e) =>
                  setForm({ ...form, day: e.target.value.replace(/\D/g, "") })
                }
                className="flex-1 h-[56px] border border-[#606060] rounded-[12px] px-4"
                inputMode="numeric"
                maxLength={2}
              />
            </div>
          </div>

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
