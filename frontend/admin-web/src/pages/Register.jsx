import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { registerUser } from "../services/authApi";

export default function Register() {
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function submit(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const password = form.get("password");
    const confirmPassword = form.get("confirmPassword");
    setError("");
    setMessage("");

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setIsSubmitting(true);
    try {
      await registerUser({
        firstName: form.get("firstName").trim(),
        lastName: form.get("lastName").trim(),
        gender: form.get("gender"),
        mobile: form.get("mobile").trim(),
        email: form.get("email").trim(),
        password,
      });
      setMessage("Your account has been created. A trusted administrator must grant ADMIN access before you can sign in here.");
      formElement.reset();
      window.setTimeout(() => navigate("/login"), 1600);
    } catch (requestError) {
      setError(requestError.message || "Unable to create the account request. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return <main className="login-layout">
    <section className="login-story">
      <Link className="brand" to="/login">everyday<span>adm.</span></Link>
      <div><p className="eyebrow">ADMINISTRATION ACCESS</p><h1>Start with<br />the right access.</h1><p>Your backend decides who receives administrator privileges.</p></div>
      <small>everyday / Administration</small>
    </section>
    <section className="login-form-panel"><form className="login-form" onSubmit={submit}>
      <p className="eyebrow">ADMINISTRATION ACCOUNT</p><h2>Create your account.</h2><p className="muted">This registration creates a User-role account in user-service.</p>
      <div className="form-row">
        <div><label htmlFor="firstName">First name</label><input id="firstName" name="firstName" type="text" placeholder="First name" autoComplete="given-name" required /></div>
        <div><label htmlFor="lastName">Last name</label><input id="lastName" name="lastName" type="text" placeholder="Last name" autoComplete="family-name" required /></div>
      </div>
      <label htmlFor="gender">Gender</label><select id="gender" name="gender" defaultValue="" required><option value="" disabled>Select gender</option><option value="Male">Male</option><option value="Female">Female</option><option value="Other">Other</option><option value="Prefer not to say">Prefer not to say</option></select>
      <label htmlFor="mobile">Mobile number</label><input id="mobile" name="mobile" type="tel" placeholder="9876543210" autoComplete="tel" inputMode="tel" pattern="[+0-9]{7,15}" required />
      <label htmlFor="email">Email address</label><input id="email" name="email" type="email" placeholder="admin@example.com" autoComplete="email" required />
      <label htmlFor="password">Password</label><input id="password" name="password" type="password" placeholder="At least 6 characters" autoComplete="new-password" required minLength={6} />
      <label htmlFor="confirmPassword">Confirm password</label><input id="confirmPassword" name="confirmPassword" type="password" placeholder="Enter your password again" autoComplete="new-password" required minLength={6} />
      {error && <p className="form-error" role="alert">{error}</p>}
      {message && <p className="form-success" role="status">{message}</p>}
      <button className="primary" type="submit" disabled={isSubmitting}>{isSubmitting ? "Submitting…" : "Request access"}<span>→</span></button>
      <p className="demo-note">This form sends profile details to <code>/api/auth/register</code> with registration type <code>USER</code>.</p>
      <p className="auth-link"><Link to="/login">← Back to sign in</Link></p>
    </form></section>
  </main>;
}
