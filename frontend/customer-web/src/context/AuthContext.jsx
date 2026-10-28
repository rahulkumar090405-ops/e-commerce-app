import { createContext, useCallback, useContext, useState } from "react";
import { apiRequest } from "../services/api";
import { exchangeGoogleCustomerToken, getCustomerProfile } from "../services/authApi";

const AuthContext = createContext(null);
const KEY = "commerce-customer-session";

function readSession() {
  try {
    const value = JSON.parse(sessionStorage.getItem(KEY));
    return typeof value?.email === "string" && typeof value?.token === "string" && value.token
      ? value
      : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readSession);
  
  async function login(email, password) {
    const response = await apiRequest("user", "/api/auth/login", {
      method: "POST",
      body: { email, password },
    });
    if (typeof response?.token !== "string" || !response.token) {
      throw new Error("Authentication failed: no token returned.");
    }
    const session = { email, token: response.token };
    try {
      sessionStorage.setItem(KEY, JSON.stringify(session));
    } catch {
      /* In-memory fallback. */
    }
    setUser(session);
    return session;
  }

  async function completeGoogleLogin() {
    const response = await exchangeGoogleCustomerToken();
    if (typeof response?.token !== "string" || !response.token) {
      throw new Error("Authentication failed: no token returned.");
    }
    const profile = await getCustomerProfile(response.token);
    if (!profile?.roles?.includes("ROLE_CUSTOMER")) {
      throw new Error("This Google account does not have customer access.");
    }
    const session = { ...profile, token: response.token };
    try {
      sessionStorage.setItem(KEY, JSON.stringify(session));
    } catch {
      /* In-memory fallback. */
    }
    setUser(session);
    return session;
  }
  function logout() {
    try {
      sessionStorage.removeItem(KEY);
    } catch {
      /* In-memory fallback. */
    }
    setUser(null);
  }

  const saveProfile = useCallback((profile) => {
    setUser((current) => {
      if (!current) return current;
      const updated = { ...current, ...profile, token: current.token };
      try { sessionStorage.setItem(KEY, JSON.stringify(updated)); } catch { /* In-memory fallback. */ }
      return updated;
    });
  }, []);
  return (
    <AuthContext.Provider value={{ user, login, completeGoogleLogin, logout, saveProfile }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
