// src/pages/Signup.jsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../lib/api.js";

export default function Signup() {
  const nav = useNavigate();
  const [form, setForm] = useState({ email: "", password: "", name: "" });
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  const onChange = (e) => {
    const { name, value } = e.target;
    setForm((s) => ({ ...s, [name]: value }));
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setErr("");

    if (!form.email || !form.password || !form.name) {
      setErr("이메일/비밀번호/이름을 모두 입력하세요.");
      return;
    }
    if (form.password.length < 4) {
      setErr("비밀번호는 4자 이상으로 입력하세요.");
      return;
    }

    try {
      setLoading(true);
      await authApi.signup(form);
      alert("회원가입 완료! 이제 로그인해 주세요.");
      nav("/login");
    } catch (e) {
      const msg =
        e?.response?.data?.message || "회원가입 중 오류가 발생했습니다.";
      setErr(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="max-w-sm mx-auto space-y-4">
      <h2 className="text-xl font-semibold">회원가입</h2>
      <form className="space-y-3" onSubmit={onSubmit}>
        <input
          name="email"
          placeholder="이메일"
          value={form.email}
          onChange={onChange}
          className="w-full border rounded px-3 py-2"
        />
        <input
          name="name"
          placeholder="이름"
          value={form.name}
          onChange={onChange}
          className="w-full border rounded px-3 py-2"
        />
        <input
          name="password"
          type="password"
          placeholder="비밀번호"
          value={form.password}
          onChange={onChange}
          className="w-full border rounded px-3 py-2"
        />
        <button disabled={loading} className="w-full border rounded px-4 py-2">
          {loading ? "처리 중..." : "가입하기"}
        </button>
      </form>
      {err && <p className="text-red-600 text-sm">{err}</p>}
    </section>
  );
}
