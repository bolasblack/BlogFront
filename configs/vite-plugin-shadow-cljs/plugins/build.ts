import type { PluginOption, ResolvedConfig } from "vite";
import { TAG } from "../constants";
import { spawnAsync } from "../utils/spawnAsync";
import type { PluginContext } from "../types";

export function createBuildPlugin(
  initContext: (config: ResolvedConfig) => Promise<void>,
  getContext: () => PluginContext,
  buildIds: string[]
): PluginOption {
  return {
    name: "shadow-cljs:build",
    configResolved: initContext,
    apply: "build",

    async buildStart() {
      const { projectRoot } = getContext();

      console.log(`${TAG} Running shadow-cljs release...`);
      await spawnAsync("shadow-cljs", ["release", ...buildIds], {
        stdio: "inherit",
        cwd: projectRoot,
      });
    },
  };
}
