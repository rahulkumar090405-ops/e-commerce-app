import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  if (user) return <Navigate to="/" replace />;
  
  async function submit(event) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const email = data.get("email").trim();
    const password = data.get("password");
    setError("");
    setSubmitting(true);
    try {
       await login(email, password);
      navigate("/", { replace: true });
    } catch (error) {
      setError(error.message);
    } finally {
      setSubmitting(false);
    }
  }
  return (
    <main className="login-page">
      <div className="login-intro">
        <p className="eyebrow">A LITTLE MORE EVERYDAY</p>
        <h1>
          Good to
          <br />
          have you here.
        </h1>
        <p>Your favorites. Your finds. Your everyday.</p>
        <span aria-hidden="true">✳</span>
      </div>
      <form className="login-form" onSubmit={submit}>
        <p className="eyebrow">YOUR ACCOUNT</p>
        <h2>Welcome back.</h2>
        <p>Sign in and make yourself at home.</p>
        <label htmlFor="email">Email address</label>
        <input
          id="email"
          name="email"
          type="email"
          placeholder="you@example.com"
          autoComplete="username"
          required
        />
        <label htmlFor="password">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          placeholder="Enter your password"
          autoComplete="current-password"
          required
          minLength={6}
        />
        <button type="submit" className="primary" disabled={submitting}>
          Sign in <span>→</span>
        </button>
        {error && <p role="alert">{error}</p>}
        <p className="auth-link">New here? <Link to="/register">Create an account</Link></p>
        <Link className="continue-link" to="/">
          ← Back to the shop
        </Link>
      </form>
    </main>
  );
}
