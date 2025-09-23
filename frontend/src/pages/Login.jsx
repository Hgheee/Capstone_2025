import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../lib/api";
import { useAuth } from "../contexts/AuthContext";

export default function Login() {
  const nav = useNavigate();
  const { login: saveUser } = useAuth();
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
      const { data } = await authApi.login(form);
      // 백엔드 응답 형태에 따라 조정 (예: { token, user: {email, name} } 또는 {email, name})
      const userPayload = {
        email: data?.user?.email || form.email,
        name: data?.user?.name || undefined,
        token: data?.token || undefined,
      };
      saveUser(userPayload);
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
    <section style={{ maxWidth: 420, margin: "0 auto" }}>
      <h2 style={{ fontSize: 20, fontWeight: 600, marginBottom: 12 }}>
        로그인
      </h2>
      <form onSubmit={onSubmit} style={{ display: "grid", gap: 10 }}>
        <input
          name="email"
          placeholder="이메일"
          value={form.email}
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
          {loading ? "처리 중..." : "로그인"}
        </button>
      </form>
      {err && <p style={{ color: "crimson", marginTop: 8 }}>{err}</p>}
    </section>
  );
}
