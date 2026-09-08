import { exportSvg, shareExport } from "../../core/export";
import { api, errorMessage, open, viewLinks } from "../../core/runtime";
import type { Exhibition } from "../../../packages/contracts";
interface PublicWork {
  publicationId: string;
  displayName: string;
  artworks: {
    title: string;
    story: string;
    mediaIds: string[];
    images?: { id: string; url: string; displayUrl?: string; kind?: string }[];
  }[];
}
Page({
  data: {
    exhibitions: [] as Exhibition[],
    selected: null as Exhibition | null,
    works: [] as PublicWork[],
    error: "",
    busy: false,
    exportPath: "",
    exportName: "",
    initialId: "",
  },
  onLoad(options: { id?: string }) {
    this.setData({ initialId: options.id ?? "" });
  },
  onShow() {
    void this.load();
  },
  async load() {
    this.setData({
      error: "",
      selected: null,
      works: [],
      exportPath: "",
      exportName: "",
    });
    try {
      this.setData({
        exhibitions: await api.get<Exhibition[]>(
          "/v1/portfolio/public/exhibitions",
        ),
      });
      if (this.data.initialId) {
        await this.openExhibition(this.data.initialId);
        this.setData({ initialId: "" });
      }
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    }
  },
  async visit(e: MiniEvent) {
    await this.openExhibition(e.currentTarget.dataset.id);
  },
  async openExhibition(id: string) {
    this.setData({ error: "", busy: true, works: [] });
    try {
      const result = await api.get<{
        exhibition: Exhibition;
        works: PublicWork[];
      }>(`/v1/portfolio/public/exhibitions/${id}`);
      for (const work of result.works) {
        for (const artwork of work.artworks) {
          const access = await api.get<{
            items: {
              id: string;
              url: string;
              thumbnailUrl?: string;
              contentType?: string;
            }[];
          }>(
            `/v1/portfolio/public/exhibitions/${result.exhibition.id}/publications/${work.publicationId}/media-access?mediaIds=${artwork.mediaIds.join(",")}`,
          );
          artwork.images = viewLinks(access.items);
        }
      }
      this.setData({ selected: result.exhibition, works: result.works });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  image(e: MiniEvent) {
    wx.previewImage({
      urls: [e.currentTarget.dataset.url],
      current: e.currentTarget.dataset.url,
    });
  },
  async shareCard() {
    if (!this.data.selected) return;
    this.setData({ busy: true, error: "" });
    try {
      const result = await exportSvg(
        `/v1/portfolio/public/exhibitions/${this.data.selected.id}/share-card`,
      );
      this.setData({
        exportPath: result.filePath,
        exportName: result.fileName,
      });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async certificate(e: MiniEvent) {
    if (!this.data.selected) return;
    this.setData({ busy: true, error: "" });
    try {
      const result = await exportSvg(
        `/v1/portfolio/public/exhibitions/${this.data.selected.id}/publications/${e.currentTarget.dataset.id}/certificate`,
      );
      this.setData({
        exportPath: result.filePath,
        exportName: result.fileName,
      });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async shareFile() {
    try {
      await shareExport(this.data.exportPath, this.data.exportName);
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    }
  },
  previewFailed() {
    this.setData({
      error: "当前设备无法预览 SVG，可使用“分享导出文件”保存原文件。",
    });
  },
  studio() {
    open(
      `/pages/studio/index?source=EXHIBITION&sourceId=${this.data.selected?.id ?? ""}`,
    );
  },
  onShareAppMessage() {
    return {
      title: this.data.selected?.title ?? "画室作品展",
      path: `/pages/exhibitions/index?id=${this.data.selected?.id ?? ""}`,
    };
  },
});
