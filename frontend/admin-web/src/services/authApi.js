import { apiRequest } from "./api.js";

export function loginUser(email, password) {
  return apiRequest("user", "/api/auth/login", {
    method: "POST",
    body: { email, password, loginType: "ADMIN" },
  });
}

export function registerUser(registration) {
  return apiRequest("user", "/api/auth/register", {
    method: "POST",
    body: { ...registration, registrationType: "USER" },
  });
}

export function getCurrentUser() {
  return apiRequest("user", "/api/users/me");
}

export function logoutUser() {
  return apiRequest("user", "/api/auth/logout", {
    method: "POST",
  });
}
