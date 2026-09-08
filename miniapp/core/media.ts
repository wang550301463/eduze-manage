import type { MediaLink } from "../../packages/contracts";
export function viewLinks<
  T extends Pick<MediaLink, "id" | "url" | "contentType" | "thumbnailUrl">,
>(items: T[]) {
  return items.map((item) => ({
    ...item,
    displayUrl:
      !item.contentType || item.contentType.startsWith("image/")
        ? item.thumbnailUrl || item.url
        : item.url,
    kind: item.contentType?.startsWith("video/")
      ? "video"
      : item.contentType?.startsWith("audio/")
        ? "audio"
        : "image",
  }));
}
