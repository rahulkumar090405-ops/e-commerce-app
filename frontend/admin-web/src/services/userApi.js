import { apiRequest } from "./api";

export function getUsers() {
  return apiRequest("user", "/api/admin/users");
}

export function updateMyProfile(profile) {
  return apiRequest("user", "/api/users/me", {
    method: "PATCH",
    body: profile,
  });
}
