import type { ChildProcess } from "child_process";

export interface ShadowCljsOptions {
  configPath?: string;
  buildIds: string[];
}

export interface BuildConfig {
  id: string;
  outputDir: string;
  modules: string[];
  runtime?: string;
}
export type BuildConfigMap = Map</* buildId */ string, BuildConfig>;

export interface PluginContext {
  projectRoot: string;
  configPath: string;
  buildConfigs: BuildConfigMap;
}

export interface ShadowGlobalState {
  process: ChildProcess | undefined;
  buildCompleteIds: Set<string>;
  notifyBuildComplete(buildId: string): void;
  onBuildComplete(listener: (buildId: string) => void): () => void;
}
