import { api, session, errorMessage, viewLinks } from "../../core/runtime";
import type { Publication, MediaLink } from "../../../packages/contracts";
import {
  assertMediaLimit,
  publicationMediaIds,
} from "../../../packages/contracts/media";
interface Report {
  id: string;
  studentId: string;
  title: string;
  summary: string;
  publications: Publication[];
  createdAt: string;
}
Page({
  image(e: MiniEvent) {
    wx.previewImage({
      urls: this.data.images
        .filter((i) => i.kind === "image")
        .map((i) => i.url),
      current: e.currentTarget.dataset.url,
    });
  },
  data: {
    publications: [] as Publication[],
    reports: [] as Report[],
    collections: [] as Report[],
    selectedIds: [] as string[],
    title: "",
    summary: "",
    opened: null as Publication | null,
    images: [] as (MediaLink & { kind: string })[],
    busy: false,
    error: "",
    success: "",
  },
  onShow() {
    this.setData({
      publications: [],
      reports: [],
      collections: [],
      selectedIds: [],
      opened: null,
      images: [],
      title: "",
      summary: "",
      error: "",
    });
    void this.load();
  },
  onHide() {
    this.setData({ opened: null, images: [] });
  },
  async load() {
    if (!session.scope.childId) {
      this.setData({ error: "请先在首页选择孩子" });
      return;
    }
    this.setData({ busy: true, error: "" });
    try {
      const [growth, collections] = await Promise.all([
        api.get<{ publications: Publication[]; reports: Report[] }>(
          `/v1/portfolio/students/${session.scope.childId}/growth`,
        ),
        api.get<Report[]>(
          `/v1/portfolio/students/${session.scope.childId}/collections`,
        ),
      ]);
      this.setData({
        publications: growth.publications,
        reports: growth.reports,
        collections,
      });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  field(e: MiniEvent) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value });
  },
  select(e: { detail: { value: string[] } }) {
    this.setData({ selectedIds: e.detail.value });
  },
  async create() {
    this.setData({ busy: true, error: "", success: "" });
    try {
      const selected = this.data.publications.filter((p) =>
        this.data.selectedIds.includes(p.id),
      );
      assertMediaLimit(publicationMediaIds(selected.map((p) => p.content)));
      await api.post(
        `/v1/portfolio/students/${session.scope.childId}/collections`,
        {
          title: this.data.title,
          summary: this.data.summary,
          publicationIds: this.data.selectedIds,
        },
      );
      this.setData({
        selectedIds: [],
        title: "",
        summary: "",
        success: "孩子的私人精选作品集已保存。",
      });
      await this.load();
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async snapshot(e: MiniEvent) {
    this.setData({ busy: true, error: "", opened: null, images: [] });
    try {
      const reportId = e.currentTarget.dataset.report;
      const collectionId = e.currentTarget.dataset.collection;
      const publicationId = e.currentTarget.dataset.id;
      const parent = reportId
        ? this.data.reports.find((r) => r.id === reportId)
        : collectionId
          ? this.data.collections.find((r) => r.id === collectionId)
          : null;
      const publication = (parent?.publications ?? this.data.publications).find(
        (p) => p.id === publicationId,
      );
      if (!publication) throw new Error("该历史版本不再可见，请刷新成长档案");
      const path = reportId
        ? `/v1/portfolio/reports/${reportId}/publications/${publicationId}/media-access`
        : `/v1/portfolio/publications/${publicationId}/media-access`;
      const access = await api.post<{ items: MediaLink[] }>(path, {
        mediaIds: publicationMediaIds([publication.content]),
      });
      this.setData({ opened: publication, images: viewLinks(access.items) });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
});
