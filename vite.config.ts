import { defineConfig } from "vite";
import { resolve } from "path";
import { shadowCljs } from "./configs/vite-plugin-shadow-cljs";

export default defineConfig({
  root: ".",
  publicDir: "assets",
  build: {
    outDir: "dist",
    emptyOutDir: true,
    rollupOptions: {
      input: {
        main: resolve(__dirname, "index.html"),
      },
    },
  },
  plugins: [shadowCljs({ buildIds: ["browser"] })],
});
