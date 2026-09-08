import {
  api,
  session,
  errorMessage,
  viewLinks,
  open,
} from "../../core/runtime";
import type {
  Lesson,
  PortfolioRecord,
  Publication,
  ClassroomEntry,
} from "../../../packages/contracts";
Page({
  data: {
    childId: "",
    entries: [] as ClassroomEntry[],
    schedule: [] as Lesson[],
    records: [] as PortfolioRecord[],
    remainingLessons: 0,
    leaves: [] as { id: string; status: string }[],
    lessonId: "",
    reason: "",
    busy: false,
    error: "",
    success: "",
    opened: null as PortfolioRecord | null,
    images: [] as { id: string; url: string; kind: string }[],
    publications: [] as Publication[],
    reports: [] as { id: string; title: string; summary: string }[],
    displayName: "",
  },
  async onLoad(options: { studentId?: string }) {
    if (options.studentId) {
      try {
        const children = await api.get<{ id: string; branchId: string }[]>(
          "/v1/academic/family/children",
        );
        const child = children.find((c) => c.id === options.studentId);
        if (!child) throw new Error("当前家庭无权查看该孩子");
        session.switchScope({ childId: child.id, branchId: child.branchId });
        this.onShow();
      } catch (e) {
        this.setData({ error: errorMessage(e) });
      }
    }
  },
  onShow() {
    this.setData({
      childId: session.scope.childId,
      entries: [],
      schedule: [],
      records: [],
      remainingLessons: 0,
      leaves: [],
      opened: null,
      images: [],
      publications: [],
      reports: [],
      displayName: "",
      reason: "",
      error: "",
    });
    void this.load();
  },
  onHide() {
    this.setData({ opened: null, images: [] });
  },
  async load() {
    if (!session.scope.childId) {
      this.setData({ error: "请先从首页选择孩子" });
      return;
    }
    this.setData({ busy: true, error: "" });
    try {
      const from = new Date();
      const to = new Date(from.getTime() + 30 * 86400000);
      const base = `/v1/academic/family/children/${session.scope.childId}`;
      const [schedule, balance, records, leaves, growth, entries] =
        await Promise.all([
          api.get<Lesson[]>(
            `${base}/schedule?from=${from.toISOString().slice(0, 10)}&to=${to.toISOString().slice(0, 10)}`,
          ),
          api.get<{ remainingLessons: number }>(`${base}/balance`),
          api.get<PortfolioRecord[]>(
            `/v1/portfolio/records?studentId=${session.scope.childId}`,
          ),
          api.get<{ id: string; status: string }[]>(`${base}/leaves`),
          api.get<{
            publications: Publication[];
            reports: { id: string; title: string; summary: string }[];
          }>(`/v1/portfolio/students/${session.scope.childId}/growth`),
          api.get<ClassroomEntry[]>(
            `/v1/portfolio/students/${session.scope.childId}/entries`,
          ),
        ]);
      this.setData({
        entries,
        schedule,
        remainingLessons: balance.remainingLessons,
        records,
        leaves,
        publications: growth.publications,
        reports: growth.reports,
      });
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    } finally {
      this.setData({ busy: false });
    }
  },
  selectLesson(e: MiniEvent) {
    this.setData({ lessonId: e.currentTarget.dataset.id });
  },
  reason(e: MiniEvent) {
    this.setData({ reason: e.detail.value });
  },
  async leave() {
    this.setData({ busy: true, error: "", success: "" });
    try {
      await api.post(
        `/v1/academic/family/children/${session.scope.childId}/leaves`,
        { lessonId: this.data.lessonId, reason: this.data.reason },
      );
      this.setData({ success: "请假申请已提交", reason: "", lessonId: "" });
      await this.load();
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async entry(e: MiniEvent) {
    this.setData({ error: "", opened: null, images: [] });
    try {
      const entry = this.data.entries.find(
        (item) => item.id === e.currentTarget.dataset.id,
      );
      if (!entry) return;
      const access = await api.post<{ items: { id: string; url: string }[] }>(
        `/v1/portfolio/records/${entry.recordId}/entries/${entry.id}/media-access`,
        { mediaIds: entry.mediaIds },
      );
      wx.previewImage({ urls: access.items.map((item) => item.url) });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    }
  },
  async publication(e: MiniEvent) {
    this.setData({ error: "", images: [] });
    try {
      const publication = this.data.publications.find(
        (p) => p.id === e.currentTarget.dataset.id,
      );
      if (!publication) return;
      const access = await api.post<{ items: { id: string; url: string }[] }>(
        `/v1/portfolio/publications/${publication.id}/media-access`,
        { mediaIds: publication.content.artworks.flatMap((a) => a.mediaIds) },
      );
      wx.previewImage({ urls: access.items.map((item) => item.url) });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    }
  },
  async record(e: MiniEvent) {
    this.setData({ busy: true, error: "", opened: null, images: [] });
    try {
      const record = await api.get<PortfolioRecord>(
        `/v1/portfolio/records/${e.currentTarget.dataset.id}`,
      );
      const mediaIds = record.content.artworks.flatMap((a) => a.mediaIds);
      const access = mediaIds.length
        ? await api.post<{
            items: { id: string; url: string; contentType?: string }[];
          }>(`/v1/portfolio/records/${record.id}/media-access`, { mediaIds })
        : { items: [] };
      this.setData({ opened: record, images: viewLinks(access.items) });
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    } finally {
      this.setData({ busy: false });
    }
  },
  displayName(e: MiniEvent) {
    this.setData({ displayName: e.detail.value });
  },
  async consent(e: MiniEvent) {
    this.setData({ busy: true, error: "", success: "" });
    try {
      const allowed = e.currentTarget.dataset.allowed === "true";
      await api.post(
        `/v1/portfolio/publications/${e.currentTarget.dataset.id}/consent`,
        { allowed, displayName: this.data.displayName || "小画家" },
      );
      this.setData({
        success: allowed
          ? "已授权此版本用于公开展览，可随时撤销"
          : "已撤销此作品的公开授权",
      });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  growth() {
    open("/pages/growth/index");
  },
  image(e: MiniEvent) {
    wx.previewImage({
      urls: this.data.images.map((i) => i.url),
      current: e.currentTarget.dataset.url,
    });
  },
});
