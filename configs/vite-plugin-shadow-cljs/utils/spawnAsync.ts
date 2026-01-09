import { type SpawnOptions, spawn } from "child_process";

export function spawnAsync(
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
