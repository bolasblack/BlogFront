import { parseEDNString } from "edn-data";
import * as fs from "fs/promises";
import { resolve } from "path";
import { DEFAULT_CONFIG_PATH } from "../constants";
import type { BuildConfig } from "../types";

function ednToJs(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(ednToJs);
  }
  if (value && typeof value === "object" && "map" in value) {
    return ednToJs((value as { map: unknown }).map);
  }
  if (value && typeof value === "object" && "key" in value) {
    return (value as { key: string }).key;
  }
  if (value && typeof value === "object" && "sym" in value) {
    return (value as { sym: string }).sym;
  }
  return value;
}

const getFieldOfMap = <T>(
  map: [string, unknown][],
  key: string
): undefined | T => {
  const pair = map.find((pair) => pair[0] === key);
  if (!pair) return undefined;
  return pair[1] as T;
};

export async function loadBuildConfigs(
  configPath: string,
  buildIds: string[]
): Promise<Map<string, BuildConfig>> {
  const content = await fs.readFile(configPath, "utf-8");
  const parsed = parseEDNString(content, {
    keywordAs: "object",
    setAs: "array",
    mapAs: "doubleArray",
  });
  const config = ednToJs(parsed) as [string, unknown][];

  const builds = getFieldOfMap<
    [buildId: string, buildConfig: [string, unknown]][]
  >(config, "builds");
  if (!builds) {
    throw new Error(`No :builds found in ${configPath}`);
  }

  const result = new Map<string, BuildConfig>();

  for (const buildId of buildIds) {
    const build = getFieldOfMap<[key: string, value: unknown][]>(
      builds,
      buildId
    );
    if (!build) {
      const available = builds.map((pair) => pair[0]).join(", ");
      throw new Error(
        `Build "${buildId}" not found in ${configPath}. Available: ${available}`
      );
    }

    const outputDir = getFieldOfMap<string>(build, "output-dir");
    if (!outputDir) {
      throw new Error(`No :output-dir found for build "${buildId}"`);
    }

    const modulesConfig = getFieldOfMap<
      [moduleName: string, moduleConfig: [string, unknown]][]
    >(build, "modules");
    if (!modulesConfig || modulesConfig.length === 0) {
      throw new Error(`No :modules found for build "${buildId}"`);
    }

    const _runtime = getFieldOfMap<string>(build, "runtime");
    const runtime =
      typeof _runtime === "string"
        ? _runtime
        : // https://github.com/shadow-cljs/shadow-cljs.github.io/blob/a0076f6c4a48f5d0c96fbe05f1c97965bdc45657/docs/target-esm.adoc?plain=1#L12
          "browser";

    result.set(buildId, {
      id: buildId,
      outputDir,
      modules: modulesConfig.map((pair) => pair[0]),
      runtime,
    });
  }

  return result;
}

export function resolveConfigPath(projectRoot: string, configPath?: string) {
  return resolve(projectRoot, configPath ?? DEFAULT_CONFIG_PATH);
}

export function isBrowserTarget(buildConfig: BuildConfig): boolean {
  return (buildConfig.runtime ?? "browser") === "browser";
}

export function getEntryPath(
  projectRoot: string,
  buildConfig: BuildConfig
): string {
  return resolve(
    projectRoot,
    buildConfig.outputDir,
    `${buildConfig.modules[0]}.js`
  );
}
