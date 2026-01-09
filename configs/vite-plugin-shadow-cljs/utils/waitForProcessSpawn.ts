import type { ChildProcess } from "child_process";

export function waitForProcessSpawn(proc: ChildProcess): Promise<void> {
  return new Promise((resolve, reject) => {
    const onSpawn = () => {
      cleanup();
      resolve();
    };

    const onError = (err: Error) => {
      cleanup();
      reject(err);
    };

    const cleanup = () => {
      proc.off("spawn", onSpawn);
      proc.off("error", onError);
    };

    proc.once("spawn", onSpawn);
    proc.once("error", onError);
  });
}
