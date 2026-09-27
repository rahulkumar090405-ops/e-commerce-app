import { createContext, useContext, useState } from "react";

const AuthContext = createContext(null);
const KEY = "commerce-admin-demo-session";

function readSession() {
  try {
    const value = JSON.parse(sessionStorage.getItem(KEY));
    return typeof value?.email === "string" ? { email: value.email } : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readSession);
  function login(email) {
    // TODO: Replace this demo session with user-service authentication and role verification.
    const session = { email };
    try {
      sessionStorage.setItem(KEY, JSON.stringify(session));
    } catch {
      /* In-memory fallback. */
    }
    setUser(session);
  }
  function logout() {
    try {
      sessionStorage.removeItem(KEY);
    } catch {
      /* In-memory fallback. */
    }
    setUser(null);
  }
  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
