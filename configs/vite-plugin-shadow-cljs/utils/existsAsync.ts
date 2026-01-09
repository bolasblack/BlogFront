import * as fs from "fs/promises";

export async function existsAsync(path: string): Promise<boolean> {
  return fs
    .access(path)
    .then(() => true)
    .catch(() => false);
}
