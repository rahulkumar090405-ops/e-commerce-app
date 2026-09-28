import { apiRequest } from "./api";

const sessionOptions = { credentials: "include" };

export function loginUser(email, password) {
  return apiRequest("user", "/api/auth/login", {
    ...sessionOptions,
    method: "POST",
    body: { email, password, loginType: "ADMIN" },
  });
}

export function registerUser(registration) {
  return apiRequest("user", "/api/auth/register", {
    ...sessionOptions,
    method: "POST",
    body: { ...registration, registrationType: "USER" },
  });
}

export function getCurrentUser() {
  return apiRequest("user", "/api/users/me", sessionOptions);
}

export function logoutUser() {
  return apiRequest("user", "/api/auth/logout", {
    ...sessionOptions,
    method: "POST",
  });
}
