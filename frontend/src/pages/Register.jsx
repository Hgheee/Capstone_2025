import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../lib/api";

export default function Register() {
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
    <section style={{ maxWidth: 420, margin: "0 auto" }}>
      <h2 style={{ fontSize: 20, fontWeight: 600, marginBottom: 12 }}>
        회원가입
      </h2>
      <form onSubmit={onSubmit} style={{ display: "grid", gap: 10 }}>
        <input
          name="email"
          placeholder="이메일"
          value={form.email}
          onChange={onChange}
        />
        <input
          name="name"
          placeholder="이름"
          value={form.name}
          onChange={onChange}
        />
        <input
          name="password"
          type="password"
          placeholder="비밀번호"
          value={form.password}
          onChange={onChange}
        />
        <button type="submit" disabled={loading}>
          {loading ? "처리 중..." : "가입하기"}
        </button>
      </form>
      {err && <p style={{ color: "crimson", marginTop: 8 }}>{err}</p>}
    </section>
  );
}
