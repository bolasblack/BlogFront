import { resolve } from "path";
import * as fs from "fs/promises";
import { createReadStream } from "fs";
import { spawn, type ChildProcess, type SpawnOptions } from "child_process";
import type { IncomingMessage, ServerResponse } from "http";
import pc from "picocolors";
import { parseEDNString } from "edn-data";
import type { PluginOption, ResolvedConfig } from "vite";

// ============================================================================
// Constants
// ============================================================================

const DEFAULT_CONFIG_PATH = "shadow-cljs.edn";
const CLJS_JS_SUFFIX = ".cljs.js";
const TAG = pc.cyan("[shadow-cljs]");

// ============================================================================
// Types
// ============================================================================

export interface ShadowCljsOptions {
  /**
   * Path to shadow-cljs.edn file, relative to project root.
   * @default "shadow-cljs.edn"
   */
  configPath?: string;

  /**
   * Build IDs as defined in shadow-cljs.edn :builds.
   * @example ["browser"]
   * @example ["browser", "worker"]
   */
  buildIds: string[];
}

interface BuildConfig {
  outputDir: string;
  modules: string[];
}

interface PluginContext {
  projectRoot: string;
  assetsDir: string;
  configPath: string;
  buildConfigs: Map<string, BuildConfig>;
}

// ============================================================================
// EDN Parsing
// ============================================================================

/**
 * Convert EDN parsed result to plain JS object.
 * edn-data returns Maps for EDN maps and uses special types for keywords/symbols.
 */
function ednToJs(value: unknown): unknown {
  if (value instanceof Map) {
    const obj: Record<string, unknown> = {};
    for (const [k, v] of value) {
      const key = ednToJs(k);
      obj[typeof key === "string" ? key : String(key)] = ednToJs(v);
    }
    return obj;
  }
  if (Array.isArray(value)) {
    return value.map(ednToJs);
  }
  if (value && typeof value === "object" && "key" in value) {
    return (value as { key: string }).key;
  }
  if (value && typeof value === "object" && "sym" in value) {
    return (value as { sym: string }).sym;
  }
  return value;
}

async function loadBuildConfigs(
  configPath: string,
  buildIds: string[]
): Promise<Map<string, BuildConfig>> {
  const content = await fs.readFile(configPath, "utf-8");
  const parsed = parseEDNString(content, { mapAs: "map", keywordAs: "object" });
  const config = ednToJs(parsed) as Record<string, unknown>;

  const builds = config.builds as Record<string, Record<string, unknown>>;
  if (!builds) {
    throw new Error(`No :builds found in ${configPath}`);
  }

  const result = new Map<string, BuildConfig>();

  for (const buildId of buildIds) {
    const build = builds[buildId];
    if (!build) {
      const available = Object.keys(builds).join(", ");
      throw new Error(
        `Build "${buildId}" not found in ${configPath}. Available: ${available}`
      );
    }

    const outputDir = build["output-dir"] as string;
    if (!outputDir) {
      throw new Error(`No :output-dir found for build "${buildId}"`);
    }

    const modulesConfig = build.modules as Record<string, unknown>;
    if (!modulesConfig) {
      throw new Error(`No :modules found for build "${buildId}"`);
    }

    result.set(buildId, {
      outputDir,
      modules: Object.keys(modulesConfig),
    });
  }

  return result;
}

// ============================================================================
// Process Utilities
// ============================================================================

function spawnAsync(
  cmd: string,
  args: string[],
  opts: SpawnOptions = {}
): Promise<void> {
  return new Promise((resolve, reject) => {
    const proc = spawn(cmd, args, opts);
    proc.on("close", (code) =>
      code === 0 ? resolve() : reject(new Error(`Exit code: ${code}`))
    );
    proc.on("error", (err) => {
      if ((err as NodeJS.ErrnoException).code === "ENOENT") {
        reject(
          new Error(
            `"${cmd}" not found. Install shadow-cljs as a dependency and run vite via npm/pnpm.`
          )
        );
      } else {
        reject(err);
      }
    });
  });
}

function pipeProcessOutput(proc: ChildProcess): void {
  proc.stdout?.on("data", (data: Buffer) => {
    for (const line of data.toString().trim().split("\n")) {
      console.log(`${TAG} ${line}`);
    }
  });

  proc.stderr?.on("data", (data: Buffer) => {
    for (const line of data.toString().trim().split("\n")) {
      console.error(`${TAG} ${pc.red(line)}`);
    }
  });

  proc.on("close", (code) => {
    if (code !== null && code !== 0) {
      console.error(
        `\n${pc.bgRed(pc.bold(" shadow-cljs crashed! "))} Exit code: ${code}\n`
      );
    }
  });
}

// ============================================================================
// HTML Utilities
// ============================================================================

function injectScripts(html: string, scripts: string[]): string {
  if (scripts.length === 0) return html;
  const tags = scripts
    .map((src) => `<script src="${src}"></script>`)
    .join("\n    ");
  return html.replace("</body>", `    ${tags}\n  </body>`);
}

/** Convert shadow-cljs output filename to Vite asset name */
function toViteAssetName(filename: string): string {
  return filename.replace(/\.js$/, CLJS_JS_SUFFIX);
}

// ============================================================================
// Plugin Implementation
// ============================================================================

/**
 * Vite plugin for integrating shadow-cljs.
 *
 * In dev mode, starts shadow-cljs watch and serves compiled JS files.
 * In build mode, runs shadow-cljs release and emits hashed assets.
 *
 * @example
 * ```ts
 * import { shadowCljs } from "./configs/vite-plugin-shadow-cljs";
 *
 * export default defineConfig({
 *   plugins: [
 *     shadowCljs({ buildIds: ["browser"] }),
 *     // or multiple builds:
 *     shadowCljs({ buildIds: ["browser", "worker"] }),
 *   ],
 * });
 * ```
 */
export function shadowCljs(options: ShadowCljsOptions): PluginOption[] {
  const { buildIds } = options;
  const ctx: Partial<PluginContext> = {};

  let shadowProcess: ChildProcess | undefined;

  async function initContext(config: ResolvedConfig): Promise<void> {
    ctx.projectRoot = config.root;
    ctx.assetsDir = config.build.assetsDir;
    ctx.configPath = resolve(
      ctx.projectRoot,
      options.configPath ?? DEFAULT_CONFIG_PATH
    );
    ctx.buildConfigs = await loadBuildConfigs(ctx.configPath, buildIds);
  }

  function getContext(): PluginContext {
    return ctx as PluginContext;
  }

  return [
    {
      name: "shadow-cljs:build",
      apply: "build",

      configResolved: initContext,

      async buildStart() {
        const { projectRoot, assetsDir, buildConfigs } = getContext();

        console.log(`${TAG} Running shadow-cljs release...`);
        await spawnAsync("shadow-cljs", ["release", ...buildIds], {
          stdio: "inherit",
          cwd: projectRoot,
        });

        for (const buildConfig of buildConfigs.values()) {
          await emitBuildAssets.call(this, projectRoot, assetsDir, buildConfig);
        }
      },

      transformIndexHtml: {
        order: "post",
        handler(html, ctx) {
          const scripts = Object.entries(ctx.bundle ?? {})
            .filter(
              ([fileName, asset]) =>
                asset.type === "asset" &&
                fileName.includes(".cljs") &&
                fileName.endsWith(".js")
            )
            .map(([fileName]) => `/${fileName}`);
          return injectScripts(html, scripts);
        },
      },
    },
    {
      name: "shadow-cljs:serve",
      apply: "serve",

      configResolved: initContext,

      transformIndexHtml(html) {
        const { buildConfigs } = getContext();
        const modules = Array.from(buildConfigs.values()).flatMap(
          (c) => c.modules
        );
        return injectScripts(
          html,
          modules.map((m) => `/${m}.js`)
        );
      },

      configureServer(server) {
        const { projectRoot, buildConfigs } = getContext();

        console.log(`${TAG} Starting shadow-cljs watch...`);
        shadowProcess = spawn("shadow-cljs", ["watch", ...buildIds], {
          stdio: ["ignore", "pipe", "pipe"],
          cwd: projectRoot,
        });

        pipeProcessOutput(shadowProcess);

        server.httpServer?.on("close", () => {
          if (shadowProcess) {
            console.log(`${TAG} Stopping shadow-cljs...`);
            shadowProcess.kill();
          }
        });

        server.middlewares.use(createDevMiddleware(projectRoot, buildConfigs));
      },
    },
  ];
}

// ============================================================================
// Build Helpers
// ============================================================================

async function emitBuildAssets(
  this: {
    emitFile: (file: {
      type: "asset";
      name?: string;
      fileName?: string;
      source: string | Buffer;
    }) => void;
  },
  projectRoot: string,
  assetsDir: string,
  buildConfig: BuildConfig
): Promise<void> {
  const outDir = resolve(projectRoot, buildConfig.outputDir);
  const files = await fs.readdir(outDir);

  for (const file of files) {
    const filePath = resolve(outDir, file);

    if (file.endsWith(".js.map")) {
      // Source maps: fixed filename (no hash needed, only devtools loads them)
      const mapName = toViteAssetName(file.replace(/\.map$/, "")) + ".map";
      this.emitFile({
        type: "asset",
        fileName: `${assetsDir}/${mapName}`,
        source: await fs.readFile(filePath),
      });
    } else if (file.endsWith(".js")) {
      // JS files: hashed name, update sourceMappingURL
      const name = toViteAssetName(file);
      // sourceMappingURL is relative to JS file location.
      // In standard Vite config, both files are in the same assetsDir.
      const mapName = name + ".map";
      const content = (await fs.readFile(filePath, "utf-8")).replace(
        /\/\/# sourceMappingURL=.+$/m,
        `//# sourceMappingURL=${mapName}`
      );
      this.emitFile({ type: "asset", name, source: content });
    }
  }
}

// ============================================================================
// Dev Server Middleware
// ============================================================================

function createDevMiddleware(
  projectRoot: string,
  buildConfigs: Map<string, BuildConfig>
) {
  return async (
    req: IncomingMessage,
    res: ServerResponse,
    next: () => void
  ) => {
    const match = req.url?.match(/^\/([^?]+\.js)(?:\?|$)/);
    if (!match) {
      return next();
    }

    const requestedFile = match[1];
    for (const buildConfig of buildConfigs.values()) {
      const filePath = resolve(
        projectRoot,
        buildConfig.outputDir,
        requestedFile
      );
      try {
        await fs.stat(filePath);
        res.setHeader("Content-Type", "application/javascript");
        createReadStream(filePath).pipe(res);
        return;
      } catch {
        // File doesn't exist in this output dir, try next
      }
    }
    next();
  };
}
