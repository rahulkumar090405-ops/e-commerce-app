import { test } from "node:test";
import assert from "node:assert/strict";
import { build } from "esbuild";
import { createRequire } from "node:module";
import vm from "node:vm";

// Render actual components without a browser.
const bundled = await build({
  stdin: {
    resolveDir: process.cwd(), sourcefile: "auth-ui-check.jsx", loader: "jsx",
    contents: `
      import React from "react";
      import { renderToStaticMarkup } from "react-dom/server";
      import { MemoryRouter } from "react-router-dom";
      import App from "./src/App.jsx";
      import { AuthContext } from "./src/context/AuthContext.jsx";
      export function renderRoute(path, session = {}) {
        const value = { user: null, isLoading: false, logout() {}, login() {}, saveCurrentUser() {}, ...session };
        return renderToStaticMarkup(<MemoryRouter initialEntries={[path]}><AuthContext.Provider value={value}><App /></AuthContext.Provider></MemoryRouter>);
      }
    `,
  },
  bundle: true, packages: "external", platform: "node", format: "cjs", jsx: "automatic",
  write: false, define: { "import.meta.env": "{}" },
});
const context = { require: createRequire(import.meta.url), module: { exports: {} }, console, process, Buffer };
vm.runInNewContext(bundled.outputFiles[0].text, context);
const { renderRoute } = context.module.exports;
test("admin login uses only the enabled email/password form", () => {
  const html = renderRoute("/login");
  assert.doesNotMatch(html, /Google|OAuth|oauth|social sign-in/i);
  assert.match(html, /type="email"/);
  assert.match(html, /type="password"/);
  assert.match(html, /autoComplete="current-password"/i);
  assert.match(html, /session stays active in this tab/i);
  assert.doesNotMatch(html, /disabled=""/);
});

test("admin login waits while a saved session is being verified", () => {
  const html = renderRoute("/login", { isLoading: true });
  assert.match(html, /Checking your administrator session/);
  assert.doesNotMatch(html, /type="password"/);
});

test("authenticated admin routes use backend account data", () => {
  const session = { user: { email: "admin@example.com", firstName: "Admin", roles: ["ROLE_ADMIN"] } };
  assert.ok(renderRoute("/dashboard", session).includes("<strong>Administrator</strong>"));
  assert.match(renderRoute("/profile", session), /Save profile/);
  assert.match(renderRoute("/users", session), /Search users/);
});
