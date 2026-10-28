import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "VITE_");
  return {
  plugins: [react(), tailwindcss()],
  server: {
    port: 5174,
    strictPort: true,
    proxy: {
      "/user-service": {
        target: env.VITE_USER_SERVICE_URL || "http://localhost:8081",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/user-service/, ""),
      },
      "/oauth2": {
        target: env.VITE_USER_SERVICE_URL || "http://localhost:8081",
        changeOrigin: true,
      },
      "/login/oauth2": {
        target: env.VITE_USER_SERVICE_URL || "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
  preview: { port: 4174, strictPort: true },
  };
});
