import { serviceUrls } from "../config/services.js";

const SESSION_TOKEN_KEY = "everyday-admin-session-token";

function readStoredToken() {
  try {
    return sessionStorage.getItem(SESSION_TOKEN_KEY);
  } catch {
    return null;
  }
}

let token = readStoredToken();
let currentUser = null;
const listeners = new Set();

export function getSessionToken() { return token; }
export function saveSessionToken(value) {
  try {
    sessionStorage.setItem(SESSION_TOKEN_KEY, value);
  } catch {
    throw new Error("Unable to save the admin session in this browser.");
  }
  token = value;
}
export function getSessionUser() { return currentUser; }
export function saveSessionUser(value) { currentUser = value; }
export function onAuthCleared(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
export function clearSessionAuth() {
  token = null;
  currentUser = null;
  try {
    sessionStorage.removeItem(SESSION_TOKEN_KEY);
  } catch { /* Storage may be unavailable. */ }
  for (const listener of listeners) listener();
}
// Clean up credentials retained by the previous implementation.
try {
  sessionStorage.removeItem("everyday-admin-auth-token");
  sessionStorage.removeItem("everyday-admin-auth-user");
} catch { /* Storage may be unavailable. */ }

export async function apiRequest(
  service,
  path,
  { body, headers, ...options } = {},
) {
  if (!serviceUrls[service]) throw new Error(`Unknown service: ${service}`);
  const requestToken = getSessionToken();
  const response = await fetch(
    `${serviceUrls[service].replace(/\/$/, "")}/${path.replace(/^\//, "")}`,
    {
      credentials: "omit",
      ...options,
      headers: {
        ...(requestToken ? { Authorization: `Bearer ${requestToken}` } : {}),
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...headers,
      },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    },
  );
  if (response.status === 401 && requestToken) clearSessionAuth();
  if (response.status === 204) return null;

  const isJson = response.headers.get("content-type")?.includes("json");
  const payload = isJson ? await response.json().catch(() => null) : await response.text();

  if (!response.ok) {
    const message = payload?.detail || payload?.message || payload?.error || (typeof payload === "string" && payload) || `${service}-service request failed (${response.status})`;
    const error = new Error(message);
    error.status = response.status;
    error.response = payload;
    throw error;
  }

  return payload;
}
