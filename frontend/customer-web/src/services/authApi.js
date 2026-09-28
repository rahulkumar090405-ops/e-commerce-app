import { apiRequest } from "./api";

export function registerCustomer(registration) {
  return apiRequest("user", "/api/auth/register", {
    method: "POST",
    body: { ...registration, registrationType: "CUSTOMER" },
  });
}
