import type { PluginOption, ResolvedConfig } from "vite";
import type { PluginContext, ShadowCljsOptions } from "../types";

export function createCljsEnvPatchPlugin(
  initContext: (config: ResolvedConfig) => Promise<void>,
  getContext: () => PluginContext,
  options: ShadowCljsOptions
): PluginOption {
  return {
    name: "shadow-cljs:cljs-env-patch",
    configResolved: initContext,
    enforce: "pre",

    async transform(code, id) {
      if (!id.includes("cljs-runtime/")) return;

      // Always inject the patch into cljs_env.js
      if (id.includes("cljs_env.js")) {
        return code + GOOG_IDEMPOTENT_PATCH;
      }

      return code;
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
