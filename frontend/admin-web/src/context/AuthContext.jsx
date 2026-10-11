import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { getCurrentUser, loginUser, logoutUser } from "../services/authApi.js";
import { clearSessionAuth, getSessionToken, onAuthCleared, saveSessionToken, saveSessionUser } from "../services/api.js";

export const AuthContext = createContext(null);

function getTokenExpiration(token) {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const claims = JSON.parse(atob(payload.padEnd(Math.ceil(payload.length / 4) * 4, "=")));
    if (!Number.isFinite(claims.exp) || claims.exp * 1000 <= Date.now()) {
      throw new Error("Your login has expired.");
    }
    return claims.exp * 1000;
  } catch (error) {
    if (error.message === "Your login has expired.") throw error;
    throw new Error("The saved admin session is invalid.");
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [sessionError, setSessionError] = useState("");
  const [expiresAt, setExpiresAt] = useState(null);

  useEffect(() => onAuthCleared(() => {
    setUser(null);
    setExpiresAt(null);
    setIsLoading(false);
  }), []);
  useEffect(() => {
    let cancelled = false;
    const token = getSessionToken();

    async function restoreSession() {
      if (!token) {
        setIsLoading(false);
        return;
      }

      try {
        const tokenExpiry = getTokenExpiration(token);
        const profile = await getCurrentUser();
        if (!profile?.roles?.includes("ROLE_ADMIN")) {
          throw new Error("Administrator access is required.");
        }
        if (cancelled) return;
        setExpiresAt(tokenExpiry);
        setUser(profile);
        saveSessionUser(profile);
      } catch (error) {
        if (cancelled) return;
        if (error.status === 401 || error.status === 403
          || error.message === "Administrator access is required."
          || error.message === "The saved admin session is invalid."
          || error.message === "Your login has expired.") {
          clearSessionAuth();
        } else {
          setSessionError("Your admin session could not be verified. Check that user-service is running, then reload.");
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    restoreSession();
    return () => { cancelled = true; };
  }, []);
  useEffect(() => {
    if (!expiresAt) return;
    const timeout = window.setTimeout(clearSessionAuth, Math.max(0, expiresAt - Date.now()));
    return () => window.clearTimeout(timeout);
  }, [expiresAt]);

  const acceptToken = useCallback(async (response) => {
    const token = response?.token;
    if (!token) throw new Error("user-service did not return an application token.");
    try {
      const tokenExpiry = getTokenExpiration(token);
      saveSessionToken(token);
      // The backend validates the bearer token and returns current database roles.
      const profile = await getCurrentUser();
      if (!profile?.roles?.includes("ROLE_ADMIN")) throw new Error("Administrator access is required.");
      setExpiresAt(tokenExpiry);
      setUser(profile);
      saveSessionUser(profile);
      return { user: profile, response };
    } catch (error) {
      clearSessionAuth();
      throw error;
    }
  }, []);

  const login = useCallback(async (email, password) => {
    setSessionError("");
    clearSessionAuth();
    return acceptToken(await loginUser(email, password));
  }, [acceptToken]);

  async function logout() {
    if (!getSessionToken()) { clearSessionAuth(); return; }
    try { if (getSessionToken()) await logoutUser(); } catch { /* Drop local credentials even offline. */ }
    clearSessionAuth();
  }

  const saveCurrentUser = useCallback((profile) => {
    setUser(profile);
    saveSessionUser(profile);
  }, []);

  return <AuthContext.Provider value={{ user, isLoading, sessionError, login, logout, saveCurrentUser }}>
    {children}
  </AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
