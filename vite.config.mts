import { defineConfig } from "vite";
import { cloudflare } from "@cloudflare/vite-plugin";
import tailwindcss from "@tailwindcss/vite";
import { shadowCljs } from "./configs/vite-plugin-shadow-cljs";

export default defineConfig({
  root: ".",
  publicDir: "assets",
  build: {
    outDir: "dist",
    emptyOutDir: true,
  },
  plugins: [
    tailwindcss(),
    shadowCljs({ buildIds: ["browser", "worker"] }),
    cloudflare({
      remoteBindings: true,
      viteEnvironment: { name: "ssr" },
      config: {
        main: "virtual:shadow-cljs/worker",
      },
    }),
  ],
});
