import { apiRequest } from "./api";

export function getMyProfile(token) {
  return apiRequest("user", "/api/users/me", {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export function updateMyProfile(profile, token) {
  return apiRequest("user", "/api/users/me", {
    method: "PATCH",
    headers: { Authorization: `Bearer ${token}` },
    body: profile,
  });
}
