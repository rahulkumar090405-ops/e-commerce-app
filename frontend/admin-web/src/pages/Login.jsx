import { Link, Navigate, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (user) return <Navigate to="/dashboard" replace />;

  async function submit(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setError("");
    setIsSubmitting(true);

    try {
      await login(form.get("email").trim(), form.get("password"));
      navigate("/dashboard", { replace: true });
    } catch (requestError) {
      setError(requestError.message || "Unable to sign in. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return <main className="login-layout">
    <section className="login-story">
      <Link className="brand" to="/login">everyday<span>adm.</span></Link>
      <div><p className="eyebrow">YOUR STORE, SIMPLIFIED</p><h1>A little clarity.<br />A lot of possibility.</h1><p>One home for your products, orders, and customers.</p></div>
      <small>everyday / Administration</small>
    </section>
    <section className="login-form-panel"><form className="login-form" onSubmit={submit}>
      <p className="eyebrow">ADMIN WORKSPACE</p><h2>Welcome back.</h2><p className="muted">Sign in with your administrator account.</p>
      <label htmlFor="email">Email address</label><input id="email" name="email" type="email" placeholder="admin@example.com" autoComplete="username" required />
      <label htmlFor="password">Password</label><input id="password" name="password" type="password" placeholder="Enter your password" autoComplete="current-password" required minLength={6} />
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="primary" type="submit" disabled={isSubmitting}>{isSubmitting ? "Signing in…" : "Sign in"}<span>→</span></button>
      <p className="demo-note">The access token from <code>user-service</code> is kept in this browser tab and sent as a Bearer token with later API requests.</p>
      <p className="auth-link">Need an account? <Link to="/register">Request administrator access</Link></p>
    </form></section>
  </main>;
}
