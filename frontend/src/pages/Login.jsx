// src/pages/Login.jsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../lib/api.js";

export default function Login() {
  const nav = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  const onChange = (e) => {
    const { name, value } = e.target;
    setForm((s) => ({ ...s, [name]: value }));
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setErr("");

    if (!form.email || !form.password) {
      setErr("이메일과 비밀번호를 입력하세요.");
      return;
    }

    try {
      setLoading(true);
      await authApi.login(form);
      localStorage.setItem("auth:user", JSON.stringify({ email: form.email }));
      alert("로그인 성공!");
      nav("/home");
    } catch (e) {
      const msg =
        e?.response?.data?.message || "로그인 실패. 정보를 확인하세요.";
      setErr(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="max-w-sm mx-auto space-y-4">
      <h2 className="text-xl font-semibold">로그인</h2>
      <form className="space-y-3" onSubmit={onSubmit}>
        <input
          name="email"
          placeholder="이메일"
          value={form.email}
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
          {loading ? "처리 중..." : "로그인"}
        </button>
      </form>
      {err && <p className="text-red-600 text-sm">{err}</p>}
    </section>
  );
}
