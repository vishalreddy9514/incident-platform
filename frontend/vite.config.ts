import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 5173,
  },
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: ["./src/test-setup.ts"],
    coverage: {
      // Reporting only, no minimum-ratio gate - matches the other two services' coverage
      // setup and the project's "meaningful coverage, not a percentage chased for its own
      // sake" stance (Phase 1 section 11).
      provider: "v8",
      reporter: ["text", "html"],
    },
  },
});
