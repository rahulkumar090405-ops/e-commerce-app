import { apiRequest } from "./api";
import { serviceUrls } from "../config/services";

function userServiceUrl(path) {
  return `${serviceUrls.user.replace(/\/$/, "")}/${path.replace(/^\//, "")}`;
}

async function readJsonResponse(response) {
  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    const message = payload?.detail || payload?.message || payload?.error || `Google sign-in failed (${response.status}).`;
    const error = new Error(message);
    error.status = response.status;
    error.response = payload;
    throw error;
  }
  return payload;
}

export function getGoogleCustomerLoginUrl() {
  return userServiceUrl("/api/auth/google/customer/start");
}

export async function exchangeGoogleCustomerToken() {
  const csrf = await readJsonResponse(await fetch(userServiceUrl("/api/auth/csrf"), {
    method: "GET",
    credentials: "include",
    headers: { Accept: "application/json" },
  }));
  if (typeof csrf?.headerName !== "string" || !csrf.headerName
      || typeof csrf?.token !== "string" || !csrf.token) {
    throw new Error("user-service did not return a valid CSRF token for Google sign-in.");
  }

  return readJsonResponse(await fetch(userServiceUrl("/api/auth/google/customer/token"), {
    method: "POST",
    credentials: "include",
    headers: {
      Accept: "application/json",
      [csrf.headerName]: csrf.token,
    },
  }));
}

export function getCustomerProfile(token) {
  return apiRequest("user", "/api/users/me", {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export function registerCustomer(registration) {
  return apiRequest("user", "/api/auth/register", {
    method: "POST",
    body: { ...registration, registrationType: "CUSTOMER" },
  });
}
