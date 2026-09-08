export const MAX_MEDIA = 100;
export function uniqueMediaIds(ids: readonly string[]): string[] {
  return [...new Set(ids)];
}
export function publicationMediaIds(
  contents: readonly {
    artworks: readonly { mediaIds: readonly string[] }[];
    audioMediaIds: readonly string[];
  }[],
): string[] {
  return uniqueMediaIds(
    contents.flatMap((content) => [
      ...content.artworks.flatMap((artwork) => artwork.mediaIds),
      ...content.audioMediaIds,
    ]),
  );
}
export function assertMediaLimit(ids: readonly string[], additional = 0): void {
  if (uniqueMediaIds(ids).length + additional > MAX_MEDIA)
    throw new Error(
      "单次内容最多关联 100 个不同媒体文件（含音频），请减少文件或拆分课堂动态。",
    );
}
