import { resolve } from "path";
import type { PluginOption, ResolvedConfig } from "vite";
import type { BuildConfig, PluginContext, ShadowCljsOptions } from "../types";
import {
  injectGoogImports,
  scanGoogDependencies,
} from "../utils/googDependencyHandler";

export function createImportsInjectorPlugin(
  initContext: (config: ResolvedConfig) => Promise<void>,
  getContext: () => PluginContext,
  options: ShadowCljsOptions
): PluginOption {
  // Local state for this plugin instance to avoid global state issues.
  // Maps output directory path to its corresponding Namespace -> Filename map.
  const dependencyMaps = new Map<string, Map<string, string>>();

  return {
    name: "shadow-cljs:imports-injector",
    configResolved: initContext,
    enforce: "pre",

    async transform(code, id) {
      if (!id.includes("cljs-runtime/")) return;

      // Always inject the patch into cljs_env.js
      if (id.includes("cljs_env.js")) {
        return code + GOOG_IDEMPOTENT_PATCH;
      }

      const ctx = getContext();
      if (!ctx.buildConfigs) return;

      let buildConfig: BuildConfig | undefined;
      // Identify which build config matches this file's path
      for (const config of ctx.buildConfigs.values()) {
        const outputDir = resolve(ctx.projectRoot, config.outputDir);
        if (id.startsWith(outputDir)) {
          buildConfig = config;
          break;
        }
      }

      if (!buildConfig) return;

      // Load or create the dependency map for this output directory
      let map = dependencyMaps.get(buildConfig.outputDir);
      if (!map) {
        const runtimeDir = resolve(
          ctx.projectRoot,
          buildConfig.outputDir,
          "cljs-runtime"
        );
        map = await scanGoogDependencies(runtimeDir);
        dependencyMaps.set(buildConfig.outputDir, map);
      }

      return injectGoogImports(code, id, map);
    },
  };
}

/**
 * Appended to cljs_env.js to make goog.provide/goog.module idempotent.
 *
 * Shadow-cljs patch-goog! does:
 * - goog.provide -> goog.constructNamespace_
 * - goog.require -> goog.module.get
 *
 * Reference: shadow-cljs src/main/shadow/cljs/devtools/client/env.cljs
 * https://github.com/thheller/shadow-cljs/blob/46f8988ba6a5f1e8f67f9a9ecf1bac5656e0a8ed/src/main/shadow/cljs/devtools/client/env.cljs#L152-L161
 */
const GOOG_IDEMPOTENT_PATCH = `
;(function() {
  if (goog.__shadowCljsIdempotentPatched__) return;
  goog.__shadowCljsIdempotentPatched__ = true;

  var origProvide = goog.provide;
  goog.provide = function(name) {
    if (goog.isProvided_(name)) return;
    return goog.constructNamespace_.call(this, name);
  };

  var origRequire = goog.require;
  goog.require = goog.module.get;
})();
`;
