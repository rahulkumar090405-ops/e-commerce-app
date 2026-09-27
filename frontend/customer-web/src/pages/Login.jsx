import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  if (user) return <Navigate to="/" replace />;
  function submit(event) {
    event.preventDefault();
    login(new FormData(event.currentTarget).get("email").trim());
    navigate("/", { replace: true });
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
        <button type="submit" className="primary">
          Sign in <span>→</span>
        </button>
        <p className="demo-note">
          Demo login: use any valid email and a password of at least 6
          characters. User-service integration is pending.
        </p>
        <Link className="continue-link" to="/">
          ← Back to the shop
        </Link>
      </form>
    </main>
  );
}
