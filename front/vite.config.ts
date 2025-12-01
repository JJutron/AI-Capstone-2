import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import mkcert from "vite-plugin-mkcert";
import path from "path";

export default defineConfig({
  plugins: [vue(), mkcert()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    https: false,
    host: true,
    port: 5173,
    proxy: {
      "/api": {
        target: process.env.VITE_API_URL || "http://52.78.47.96:8080",
        changeOrigin: true,
        secure: false,
        rewrite: (p) => p,
      },
    },
  },
});