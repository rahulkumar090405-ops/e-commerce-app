import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const callbackErrors = {
  customer_access_denied: "Google sign-in needs a verified email. If this email already has an account, sign in with that account first.",
  cancelled: "Google sign-in was cancelled.",
  login_failed: "Google sign-in could not be completed. Try again or use email and password.",
};

export default function AuthCallback() {
  const { completeGoogleLogin } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const exchangeStarted = useRef(false);
  const [error, setError] = useState(() => {
    const code = searchParams.get("error");
    return code ? callbackErrors[code] || "Google sign-in could not be completed." : "";
  });

  useEffect(() => {
    if (searchParams.has("error") || exchangeStarted.current) return;
    exchangeStarted.current = true;
    completeGoogleLogin()
      .then(() => navigate("/", { replace: true }))
      .catch((requestError) => setError(requestError.message || "Unable to complete Google sign-in."));
  }, [completeGoogleLogin, navigate, searchParams]);

  return (
    <main className="google-callback">
      <section className="google-callback-card" aria-live="polite">
        <p className="eyebrow">YOUR ACCOUNT</p>
        <h1>{error ? "Sign-in unsuccessful" : "Finishing sign-in…"}</h1>
        {error ? (
          <>
            <p className="form-error" role="alert">{error}</p>
            <Link className="continue-link" to="/login">Return to sign in</Link>
          </>
        ) : (
          <p>Verifying your Google account with user-service.</p>
        )}
      </section>
    </main>
  );
}
