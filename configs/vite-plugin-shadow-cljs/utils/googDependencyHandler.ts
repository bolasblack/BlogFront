import * as fs from "fs/promises";
import { resolve, sep as pathSep } from "path";
import { TAG } from "../constants";

/**
 * Scans the ClojureScript runtime directory to build a mapping between 
 * Google Closure namespaces and their corresponding JavaScript filenames.
 * 
 * @param cljsRuntimeDir The absolute path to the cljs-runtime directory.
 * @returns A Map where keys are namespaces (e.g., "goog.debug.Error") and values are filenames.
 */
export async function scanGoogDependencies(
  cljsRuntimeDir: string
): Promise<Map<string, string>> {
  const map = new Map<string, string>();
  try {
    const files = await fs.readdir(cljsRuntimeDir);
    await Promise.all(
      files.map(async (file) => {
        if (!file.endsWith(".js")) return;
        const content = await fs.readFile(
          resolve(cljsRuntimeDir, file),
          "utf-8"
        );
        // Match goog.provide("...") or goog.module("...")
        const match = /goog\.(?:provide|module)\s*\(\s*['"]([^'"]+)['"]/.exec(
          content
        );
        if (match) {
          map.set(match[1], file);
        }
      })
    );
  } catch (e) {
    console.warn(`${TAG} Failed to scan dependencies in ${cljsRuntimeDir}`, e);
  }
  return map;
}

/**
 * Injects ESM import statements into ClojureScript runtime files based on goog.require calls.
 *
 * ### Why is this necessary?
 *
 * Shadow-cljs (when using :target :esm) generates a main entry file (e.g., worker.js) that
 * contains a list of imports for all required namespaces in the correct order. In a standard
 * browser environment, this is usually sufficient.
 *
 * However, in certain environments like Cloudflare Workers (using the Workerd runtime),
 * the module loader might execute scripts in a way that doesn't strictly guarantee that
 * a dependency's side effects (like registering a namespace in Google Closure Library's
 * global `goog.loadedModules_`) are fully completed before a dependent module starts executing,
 * especially when those modules use `goog.loadModule` internally.
 *
 * When a module calls `goog.inherits(Child, Parent)`, it expects `Parent` to be already
 * defined. If the file providing `Parent` hasn't finished executing its `goog.loadModule`
 * wrapper, `Parent` will be `undefined`, leading to:
 * `TypeError: Cannot read properties of null (reading 'prototype')`
 *
 * By injecting explicit ESM `import` statements at the top of each file that match its
 * `goog.require` calls, we leverage the JavaScript engine's native module loading system
 * to guarantee that dependencies are fully loaded and executed before the dependent module runs.
 * 
 * @param code The source code of the file.
 * @param id The absolute path of the file.
 * @param dependencyMap The namespace-to-filename mapping.
 * @returns The transformed code with injected imports.
 */
export function injectGoogImports(
  code: string,
  id: string,
  dependencyMap: Map<string, string>
): string {
  let header = "";

  // Ensure cljs_env.js is always loaded first as it initializes the goog object
  if (!code.includes('import "./cljs_env.js"')) {
    header += 'import "./cljs_env.js";\n';
  }

  // Regex to find goog.require("Namespace")
  const requireRegex = /goog\.require\s*\(\s*['"]([^'"]+)['"]\s*\)/g;
  let match;
  const imports = new Set<string>();

  while ((match = requireRegex.exec(code)) !== null) {
    const namespace = match[1];
    const filename = dependencyMap.get(namespace);

    // Only inject import if:
    // 1. We found the file in the runtime directory
    // 2. It's not the file we are currently transforming
    if (filename && !id.endsWith(pathSep + filename)) {
      // Use relative import "./filename.js"
      imports.add(`import "./${filename}";`);
    }
  }

  if (imports.size > 0) {
    header += Array.from(imports).join("\n") + "\n";
  }

  return header + code;
}
