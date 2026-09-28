import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { getCurrentUser, loginUser, logoutUser } from "../services/authApi";
import {
  clearSessionAuth,
  getSessionToken,
  getSessionUser,
  saveSessionToken,
  saveSessionUser,
} from "../services/api";

const AuthContext = createContext(null);

function getUserFromResponse(response) {
  const data = response?.data ?? response;
  if (data?.user) return data.user;
  if (!data || typeof data !== "object" || Array.isArray(data)) return null;
  return data.id || data.email || data.username || data.role || data.roles ? data : null;
}

function getTokenFromResponse(response) {
  const data = response?.data ?? response;
  return data?.token || data?.accessToken || response?.token || response?.accessToken || null;
}

function getTokenClaims(token) {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const paddedPayload = payload.padEnd(Math.ceil(payload.length / 4) * 4, "=");
    return JSON.parse(atob(paddedPayload));
  } catch {
    return {};
  }
}

function createSessionUser(response, token, email) {
  const responseUser = getUserFromResponse(response);
  const claims = getTokenClaims(token);
  const user = responseUser && typeof responseUser === "object" && !Array.isArray(responseUser)
    ? responseUser
    : claims;

  return {
    ...user,
    email: user?.email || user?.username || user?.sub || email,
  };
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!getSessionToken()) {
      setIsLoading(false);
      return;
    }

    const storedUser = getSessionUser();
    if (storedUser) setUser(storedUser);

    getCurrentUser()
      .then(response => {
        const currentUser = getUserFromResponse(response);
        if (currentUser && typeof currentUser === "object") {
          setUser(currentUser);
          saveSessionUser(currentUser);
        }
      })
      .catch(error => {
        if (error.status === 401) {
          clearSessionAuth();
          setUser(null);
        }
      })
      .finally(() => setIsLoading(false));
  }, []);

  async function login(email, password) {
    const response = await loginUser(email, password);
    const token = getTokenFromResponse(response);
    if (!token) {
      throw new Error("Login succeeded, but user-service did not return a token.");
    }

    saveSessionToken(token);
    const currentUser = createSessionUser(response, token, email);
    setUser(currentUser);
    saveSessionUser(currentUser);
    return { user: currentUser, response };
  }

  async function logout() {
    try { await logoutUser(); } catch { /* Clear the local UI even if the network call fails. */ }
    clearSessionAuth();
    setUser(null);
  }

  const saveCurrentUser = useCallback((currentUser) => {
    setUser(currentUser);
    saveSessionUser(currentUser);
  }, []);

  return (
    <AuthContext.Provider value={{ user, isLoading, login, logout, saveCurrentUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
