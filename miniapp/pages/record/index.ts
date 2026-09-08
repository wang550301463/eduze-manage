import { AudioCapture, type RecordingFile } from "../../core/audio";
import {
  assertMediaLimit,
  publicationMediaIds,
} from "../../../packages/contracts/media";
import {
  api,
  session,
  errorMessage,
  upload,
  viewLinks,
} from "../../core/runtime";
import type {
  PortfolioRecord,
  DraftInput,
  Progress,
  Theme,
  ClassroomEntry,
} from "../../../packages/contracts";
Page({
  capture: null as AudioCapture | null,
  loadedRevision: -1,
  image(e: MiniEvent) {
    wx.previewImage({
      urls: this.data.images
        .filter((i) => i.kind === "image")
        .map((i) => i.url),
      current: e.currentTarget.dataset.url,
    });
  },
  data: {
    id: "",
    recording: false,
    failedAudio: null as RecordingFile | null,
    entryMediaIds: [] as string[],
    entryKey: `entry-${Date.now()}`,
    lessonIds: [] as string[],
    lessonLabels: [] as string[],
    lessonIndex: 0,
    entryNotes: "",
    entries: [] as ClassroomEntry[],
    record: null as PortfolioRecord | null,
    draft: null as DraftInput | null,
    dirty: false,
    busy: false,
    error: "",
    success: "",
    preview: false,
    images: [] as { id: string; url: string; kind: string }[],
    progressLabels: ["未开始", "创作中", "待补课", "已完成"],
    progressIndex: 1,
    failed: [] as { tempFilePath: string; size: number; fileType: string }[],
  },
  onLoad(options: { id: string }) {
    this.capture = new AudioCapture(
      wx.getRecorderManager(),
      () => session.revision,
      (file) => {
        this.setData({ recording: false });
        void this.uploadAudio(file);
      },
      (error) => this.setData({ recording: false, error }),
    );
    this.setData({ id: options.id });
    void this.load();
  },
  onShow() {
    if (
      this.loadedRevision !== -1 &&
      this.loadedRevision !== session.revision
    ) {
      this.capture?.cancel();
      this.loadedRevision = -1;
      this.setData({
        record: null,
        draft: null,
        entries: [],
        images: [],
        failed: [],
        failedAudio: null,
        preview: false,
        dirty: false,
        entryNotes: "",
        entryMediaIds: [],
        lessonIds: [],
        lessonLabels: [],
        success: "",
        error: "身份或校区已切换，请重新打开学生记录",
      });
    }
  },
  onUnload() {
    this.capture?.dispose();
    this.capture = null;
  },
  onHide() {
    this.capture?.cancel();
    this.setData({ recording: false });
    this.setData({ images: [], preview: false });
  },
  async load() {
    this.setData({ busy: true, error: "", preview: false, images: [] });
    try {
      const record = await api.get<PortfolioRecord>(
        `/v1/portfolio/records/${this.data.id}`,
      );
      session.switchScope({ branchId: record.branchId });
      const [theme, entries] = await Promise.all([
        api.get<Theme>(`/v1/teaching/themes/${record.content.themeId}`),
        api.get<ClassroomEntry[]>(`/v1/portfolio/records/${record.id}/entries`),
      ]);
      this.setData({
        lessonIds: theme.content.lessonIds,
        lessonLabels: theme.content.lessonIds.map(
          (_, i) => `第 ${i + 1} 次关联课堂`,
        ),
        entries,
      });
      this.loadedRevision = session.revision;
      this.setData({
        record,
        draft: { ...record.content, version: record.version },
        dirty: false,
        progressIndex: [
          "NOT_STARTED",
          "IN_PROGRESS",
          "MAKEUP_PENDING",
          "COMPLETED",
        ].indexOf(record.content.progress),
      });
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    } finally {
      this.setData({ busy: false });
    }
  },
  text(e: MiniEvent) {
    const key = e.currentTarget.dataset.field;
    this.setData({
      draft: { ...this.data.draft, [key]: e.detail.value },
      dirty: true,
      preview: false,
    });
  },
  progress(e: MiniEvent) {
    const index = Number(e.detail.value);
    this.setData({
      progressIndex: index,
      draft: {
        ...this.data.draft,
        progress: ["NOT_STARTED", "IN_PROGRESS", "MAKEUP_PENDING", "COMPLETED"][
          index
        ] as Progress,
      },
      dirty: true,
      preview: false,
    });
  },
  entryPhotos(e: { detail: { value: string[] } }) {
    this.setData({ entryMediaIds: e.detail.value });
  },
  entryNotes(e: MiniEvent) {
    this.setData({ entryNotes: e.detail.value });
  },
  entryLesson(e: MiniEvent) {
    this.setData({ lessonIndex: Number(e.detail.value) });
  },
  async saveEntry() {
    this.setData({ busy: true, error: "" });
    try {
      await api.post(`/v1/portfolio/records/${this.data.id}/entries`, {
        lessonId: this.data.lessonIds[this.data.lessonIndex],
        occurredAt: new Date().toISOString(),
        notes: this.data.entryNotes,
        mediaIds: this.data.entryMediaIds,
        idempotencyKey: this.data.entryKey,
      });
      this.setData({
        entryNotes: "",
        entryMediaIds: [],
        entryKey: `entry-${Date.now()}`,
        success: "本次课堂动态已保存为草稿",
        entries: await api.get<ClassroomEntry[]>(
          `/v1/portfolio/records/${this.data.id}/entries`,
        ),
      });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async publishEntry(e: MiniEvent) {
    this.setData({ busy: true, error: "" });
    try {
      await api.post(
        `/v1/portfolio/records/${this.data.id}/entries/${e.currentTarget.dataset.id}/publish`,
        { idempotencyKey: `entry-publish-${e.currentTarget.dataset.id}` },
      );
      this.setData({
        success: "本次课堂动态已发布",
        entries: await api.get<ClassroomEntry[]>(
          `/v1/portfolio/records/${this.data.id}/entries`,
        ),
      });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  startAudio() {
    try {
      if (!this.data.draft) return;
      assertMediaLimit(publicationMediaIds([this.data.draft]), 1);
      const revision = session.revision;
      wx.authorize({
        scope: "scope.record",
        success: () => {
          if (revision !== session.revision || !this.data.draft) return;
          this.capture?.start();
          this.setData({ recording: true, error: "" });
        },
        fail: () =>
          this.setData({
            error: "录音需要麦克风权限，请在微信设置中允许后重试。",
          }),
      });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    }
  },
  stopAudio() {
    this.capture?.stop();
  },
  async uploadAudio(file: RecordingFile) {
    this.setData({ busy: true, error: "" });
    try {
      const draft = this.data.draft;
      if (!draft) return;
      assertMediaLimit(publicationMediaIds([draft]), 1);
      const id = await upload(file.tempFilePath, file.fileSize, "audio");
      this.setData({
        draft: {
          ...this.data.draft,
          audioMediaIds: [...(this.data.draft?.audioMediaIds ?? []), id],
        },
        dirty: true,
        failedAudio: null,
        success: "原始录音已上传，请保存云端草稿。",
      });
    } catch (error) {
      this.setData({ failedAudio: file, error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  retryAudio() {
    if (this.data.failedAudio) void this.uploadAudio(this.data.failedAudio);
  },
  async save() {
    this.setData({ busy: true, error: "", success: "" });
    try {
      if (this.data.draft)
        assertMediaLimit(publicationMediaIds([this.data.draft]));
      const record = await api.post<PortfolioRecord>(
        `/v1/portfolio/records/${this.data.id}`,
        this.data.draft,
        "PUT",
      );
      this.loadedRevision = session.revision;
      this.setData({
        record,
        draft: { ...record.content, version: record.version },
        dirty: false,
        success: "已保存到云端，电脑可以继续整理",
      });
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    } finally {
      this.setData({ busy: false });
    }
  },
  choose() {
    wx.chooseMedia({
      count: 9,
      mediaType: ["image", "video"],
      sourceType: ["album", "camera"],
      success: (r) => void this.uploadFiles(r.tempFiles),
      fail: (e) => this.setData({ error: e.errMsg }),
    });
  },
  async uploadFiles(
    files: { tempFilePath: string; size: number; fileType: string }[],
  ) {
    try {
      if (this.data.draft)
        assertMediaLimit(publicationMediaIds([this.data.draft]), files.length);
    } catch (error) {
      this.setData({ error: errorMessage(error) });
      return;
    }
    this.setData({ busy: true, error: "", failed: [] });
    const failed = [];
    for (const file of files) {
      try {
        const id = await upload(file.tempFilePath, file.size, file.fileType);
        const draft = this.data.draft;
        if (draft)
          this.setData({
            draft: {
              ...draft,
              artworks: [
                ...draft.artworks,
                {
                  id: `work-${Date.now()}-${id}`,
                  title: "我的创作",
                  kind: "PROCESS",
                  mediaIds: [id],
                  participantIds: [draft.studentId],
                  story: "",
                },
              ],
            },
            dirty: true,
            preview: false,
          });
      } catch (e) {
        failed.push(file);
        this.setData({ error: errorMessage(e) });
      }
    }
    this.setData({ busy: false, failed });
  },
  retry() {
    void this.uploadFiles(this.data.failed);
  },
  artwork(e: MiniEvent) {
    const index = Number(e.currentTarget.dataset.index);
    const field = e.currentTarget.dataset.field;
    const draft = this.data.draft;
    if (draft)
      this.setData({
        draft: {
          ...draft,
          artworks: draft.artworks.map((a, i) =>
            i === index ? { ...a, [field]: e.detail.value } : a,
          ),
        },
        dirty: true,
        preview: false,
      });
  },
  stage(e: MiniEvent) {
    const index = Number(e.currentTarget.dataset.index);
    const draft = this.data.draft;
    if (draft)
      this.setData({
        draft: {
          ...draft,
          artworks: draft.artworks.map((a, i) =>
            i === index
              ? { ...a, kind: a.kind === "PROCESS" ? "FINAL" : "PROCESS" }
              : a,
          ),
        },
        dirty: true,
        preview: false,
      });
  },
  async preview() {
    this.setData({ busy: true, error: "" });
    try {
      const ids = this.data.draft ? publicationMediaIds([this.data.draft]) : [];
      const access = ids.length
        ? await api.post<{
            items: { id: string; url: string; contentType?: string }[];
          }>(`/v1/portfolio/records/${this.data.id}/media-access`, {
            mediaIds: ids,
          })
        : { items: [] };
      this.setData({ preview: true, images: viewLinks(access.items) });
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async publish() {
    this.setData({ busy: true, error: "" });
    try {
      const version = this.data.record?.version;
      await api.post(`/v1/portfolio/records/${this.data.id}/publish`, {
        version,
        idempotencyKey: `publish-${this.data.id}-${version}`,
      });
      this.setData({ success: "课效已发布到授权家庭", preview: false });
      await this.load();
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    } finally {
      this.setData({ busy: false });
    }
  },
});
