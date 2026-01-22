import * as fs from "fs/promises";
import { resolve } from "path";
import type { PluginOption, ResolvedConfig } from "vite";
import type { PluginContext, ShadowCljsOptions } from "../types";
import { existsAsync } from "../utils/existsAsync";
import { getEntryPath, isBrowserTarget } from "../utils/shadowCljsConfig";
import {
  getGlobalState,
  waitForBuildComplete,
} from "../utils/shadowCljsProcess";
import {
  isShadowCljsVirtualModule,
  parseBuildIdFromVirtualId,
  sendHmrUpdate,
} from "../utils/virtual";

export function createVirtualModulePlugin(
  initContext: (config: ResolvedConfig) => Promise<void>,
  getContext: () => PluginContext,
  options: ShadowCljsOptions
): PluginOption {
  return {
    name: "shadow-cljs:virtual-module",
    configResolved: initContext,

    async resolveId(id) {
      if (!isShadowCljsVirtualModule(id)) return;
      return `\0${id}`;
    },

    async load(id) {
      const buildId = parseBuildIdFromVirtualId(id);
      if (!buildId) return;

      const ctx = getContext();
      if (!ctx.buildConfigs) {
        throw new Error("shadow-cljs plugin not initialized");
      }

      const buildConfig = ctx.buildConfigs.get(buildId);
      if (!buildConfig) {
        throw new Error(`Build '${buildId}' not found in shadow-cljs config`);
      }

      const filePath = getEntryPath(ctx.projectRoot, buildConfig);

      while (!(await existsAsync(filePath))) {
        if (!getGlobalState()) {
          throw new Error(
            `Build output file not found: ${filePath}.\nEnsure shadow-cljs release succeeded.`
          );
        }
        await waitForBuildComplete(buildId);
      }

      const content = await fs.readFile(filePath, "utf-8");
      const appendContent = isBrowserTarget(buildConfig)
        ? /* delegate HMR to shadow-cljs */ PREVENT_VITE_HMR_FULL_RELOAD
        : "";

      if (/\bexport default /m.test(content)) {
        return `export * from "${filePath}";\nexport { default } from "${filePath}";\n${appendContent}`;
      }

      return `export * from "${filePath}";\n${appendContent}`;
    },

    // Intercept HMR for shadow-cljs output files
    hotUpdate(hmrCtx) {
      const ctx = getContext();

      for (const buildConfig of ctx.buildConfigs.values()) {
        const outputDir = resolve(ctx.projectRoot, buildConfig.outputDir);
        if (!hmrCtx.file.startsWith(outputDir)) continue;

        // Browser targets: let shadow-cljs handle HMR, skip Vite's HMR
        if (isBrowserTarget(buildConfig)) {
          return [];
        }

        // Non-browser targets: queue HMR update
        // The file is already invalidated by Vite, just queue our HMR update
        void sendHmrUpdate(getContext, hmrCtx.server, buildConfig.id);

        // Return empty array to prevent Vite's default HMR (full reload)
        return [];
      }
    },
  };
}

const PREVENT_VITE_HMR_FULL_RELOAD = `
;(function() {
  if (import.meta.hot) {
    import.meta.hot.accept((mod) => {
      console.log('[ShadowCLJS Patch] mod updated', mod);
    });
  }
})();
`;
