import { serviceUrls } from "../config/services";

const TOKEN_KEY = "everyday-admin-auth-token";
const USER_KEY = "everyday-admin-auth-user";

export function getSessionToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function saveSessionToken(token) {
  try {
    sessionStorage.setItem(TOKEN_KEY, token);
  } catch {
    /* The current browser does not allow session storage. */
  }
}

export function getSessionUser() {
  try {
    const stored = JSON.parse(sessionStorage.getItem(USER_KEY));
    return stored && typeof stored === "object" ? stored : null;
  } catch {
    return null;
  }
}

export function saveSessionUser(user) {
  try {
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
  } catch {
    /* The current browser does not allow session storage. */
  }
}

export function clearSessionAuth() {
  try {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
  } catch {
    /* The current browser does not allow session storage. */
  }
}

// Integration helper; demo screens do not make backend requests.
// Define your endpoint paths and authentication policy when connecting services.
export async function apiRequest(
  service,
  path,
  { body, headers, ...options } = {},
) {
  if (!serviceUrls[service]) throw new Error(`Unknown service: ${service}`);
  const token = getSessionToken();
  const response = await fetch(
    `${serviceUrls[service].replace(/\/$/, "")}/${path.replace(/^\//, "")}`,
    {
      ...options,
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...headers,
      },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    },
  );
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
