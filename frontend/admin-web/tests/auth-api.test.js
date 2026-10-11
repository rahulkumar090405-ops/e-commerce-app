import { afterEach, test } from "node:test";
import assert from "node:assert/strict";

const originalFetch = globalThis.fetch;
const storage = new Map();
Object.defineProperty(globalThis, "sessionStorage", {
  configurable: true,
  value: {
    getItem: (key) => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, String(value)),
    removeItem: (key) => storage.delete(key),
  },
});

const {
  apiRequest,
  clearSessionAuth,
  getSessionToken,
  getSessionUser,
  onAuthCleared,
  saveSessionToken,
} = await import("../src/services/api.js");
const { getCurrentUser, loginUser, logoutUser } = await import("../src/services/authApi.js");

afterEach(() => {
  globalThis.fetch = originalFetch;
  clearSessionAuth();
});

test("email/password login, profile request, and logout use the user-service API", async () => {
  const requests = [];
  globalThis.fetch = async (url, options) => {
    requests.push({ url, options });
    const payload = requests.length === 1
      ? { token: "application-token" }
      : { email: "admin@example.com", roles: ["ROLE_ADMIN"] };
    return requests.length === 3
      ? new Response(null, { status: 204 })
      : new Response(JSON.stringify(payload), { headers: { "Content-Type": "application/json" } });
  };

  assert.deepEqual(await loginUser("admin@example.com", "test-password"), { token: "application-token" });
  assert.ok(requests[0].url.endsWith("/api/auth/login"));
  assert.deepEqual(JSON.parse(requests[0].options.body), {
    email: "admin@example.com",
    password: "test-password",
    loginType: "ADMIN",
  });
  assert.equal(requests[0].options.headers.Authorization, undefined);

  saveSessionToken("application-token");
  assert.deepEqual(await getCurrentUser(), { email: "admin@example.com", roles: ["ROLE_ADMIN"] });
  assert.equal(requests[1].url.endsWith("/api/users/me"), true);
  assert.equal(requests[1].options.headers.Authorization, "Bearer application-token");

  assert.equal(await logoutUser(), null);
  assert.ok(requests[2].url.endsWith("/api/auth/logout"));
  assert.equal(requests[2].options.method, "POST");
  assert.ok(requests.every((request) => request.options.credentials === "omit"));
});

test("unauthorized API responses clear the current session", async () => {
  saveSessionToken("application-token");
  globalThis.fetch = async (_url, options) => {
    assert.equal(options.headers.Authorization, "Bearer application-token");
    return new Response(null, { status: 401 });
  };

  let cleared = false;
  const unsubscribe = onAuthCleared(() => { cleared = true; });
  await assert.rejects(apiRequest("user", "/api/users/me"), (error) => error.status === 401);
  unsubscribe();

  assert.equal(cleared, true);
  assert.equal(getSessionToken(), null);
  assert.equal(getSessionUser(), null);
});

test("admin token survives a page reload and is removed on sign out", async () => {
  saveSessionToken("application-token");
  const reloadedApi = await import("../src/services/api.js?reload-check");

  assert.equal(storage.get("everyday-admin-session-token"), "application-token");
  assert.equal(reloadedApi.getSessionToken(), "application-token");

  clearSessionAuth();
  assert.equal(storage.has("everyday-admin-session-token"), false);
});
