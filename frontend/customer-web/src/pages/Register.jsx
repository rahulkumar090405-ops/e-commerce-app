import { Link, Navigate, useNavigate } from "react-router-dom";
import { useState } from "react";
import { registerCustomer } from "../services/authApi";
import { useAuth } from "../context/AuthContext";

export default function Register() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to="/" replace />;

  async function submit(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const password = form.get("password");

    setError("");
    setMessage("");
    if (password !== form.get("confirmPassword")) {
      setError("Passwords do not match.");
      return;
    }

    setSubmitting(true);
    try {
      await registerCustomer({
        firstName: form.get("firstName").trim(),
        lastName: form.get("lastName").trim(),
        gender: form.get("gender"),
        mobile: form.get("mobile").trim(),
        email: form.get("email").trim(),
        password,
      });
      formElement.reset();
      setMessage("Your customer account has been created. You can now sign in.");
      window.setTimeout(() => navigate("/login"), 1600);
    } catch (requestError) {
      setError(requestError.message || "Unable to create your account. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="login-page">
      <div className="login-intro">
        <p className="eyebrow">JOIN EVERYDAY</p>
        <h1>Create your<br />account.</h1>
        <p>Save your details now. Orders and account features can be connected when your backend APIs are ready.</p>
        <span aria-hidden="true">✳</span>
      </div>
      <form className="login-form" onSubmit={submit}>
        <p className="eyebrow">CUSTOMER ACCOUNT</p>
        <h2>Good to meet you.</h2>
        <p>Enter your details to create a customer account.</p>
        <div className="form-row">
          <div><label htmlFor="firstName">First name</label><input id="firstName" name="firstName" type="text" autoComplete="given-name" required /></div>
          <div><label htmlFor="lastName">Last name</label><input id="lastName" name="lastName" type="text" autoComplete="family-name" required /></div>
        </div>
        <label htmlFor="gender">Gender</label>
        <select id="gender" name="gender" defaultValue="" required>
          <option value="" disabled>Select gender</option><option value="Male">Male</option><option value="Female">Female</option><option value="Other">Other</option><option value="Prefer not to say">Prefer not to say</option>
        </select>
        <label htmlFor="mobile">Mobile number</label><input id="mobile" name="mobile" type="tel" placeholder="9876543210" autoComplete="tel" inputMode="tel" pattern="[+0-9]{7,15}" required />
        <label htmlFor="email">Email address</label><input id="email" name="email" type="email" placeholder="you@example.com" autoComplete="email" required />
        <label htmlFor="password">Password</label><input id="password" name="password" type="password" placeholder="At least 6 characters" autoComplete="new-password" minLength={6} required />
        <label htmlFor="confirmPassword">Confirm password</label><input id="confirmPassword" name="confirmPassword" type="password" placeholder="Enter your password again" autoComplete="new-password" minLength={6} required />
        {error && <p className="form-error" role="alert">{error}</p>}
        {message && <p className="form-success" role="status">{message}</p>}
        <button type="submit" className="primary" disabled={submitting}>{submitting ? "Creating account…" : "Create account"}<span>→</span></button>
        <p className="auth-link">Already have an account? <Link to="/login">Sign in</Link></p>
      </form>
    </main>
  );
}
