import { Link, Navigate, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const { user, isLoading, sessionError, login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const isBusy = isSubmitting;

  if (isLoading) return <main className="auth-loading">Checking your administrator session…</main>;
  if (user) return <Navigate to="/dashboard" replace />;

  async function submit(event) {
    event.preventDefault();
    if (isBusy) return;
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
    <section className="login-form-panel"><form className="login-form" onSubmit={submit} aria-busy={isBusy}>
      <p className="eyebrow">ADMIN WORKSPACE</p><h2>Welcome back.</h2><p className="muted">Sign in with your administrator account.</p>
      <label htmlFor="email">Email address</label><input id="email" name="email" type="email" placeholder="admin@example.com" autoComplete="username" required disabled={isBusy} />
      <label htmlFor="password">Password</label><input id="password" name="password" type="password" placeholder="Enter your password" autoComplete="current-password" required minLength={6} disabled={isBusy} />
      {sessionError && <p className="form-error" role="alert">{sessionError}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="primary" type="submit" disabled={isBusy}>{isSubmitting ? "Signing in…" : "Sign in"}<span>→</span></button>
      <p className="demo-note">Your administrator session stays active in this tab until it expires or you sign out.</p>
      <p className="auth-link">Need an account? <Link to="/register">Request administrator access</Link></p>
    </form></section>
  </main>;
}
