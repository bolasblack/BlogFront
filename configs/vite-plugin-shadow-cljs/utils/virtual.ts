const VIRTUAL_MODULE_PREFIX = "virtual:shadow-cljs/";

export type ShadowCljsVirtualId = string & { __shadowCljsVirtualId: true };

export function isShadowCljsVirtualModule(
  id: string
): id is ShadowCljsVirtualId {
  return id.startsWith(`${VIRTUAL_MODULE_PREFIX}`);
}

export function toResolvedVirtualId(buildId: string): ShadowCljsVirtualId {
  return `\0${VIRTUAL_MODULE_PREFIX}${buildId}` as any;
}

export function parseBuildIdFromVirtualId(id: ShadowCljsVirtualId): string;
export function parseBuildIdFromVirtualId(id: string): string | null;
export function parseBuildIdFromVirtualId(id: string): string | null {
  if (id.startsWith(`\0${VIRTUAL_MODULE_PREFIX}`)) {
    return id.slice(`\0${VIRTUAL_MODULE_PREFIX}`.length);
  }
  return null;
}
