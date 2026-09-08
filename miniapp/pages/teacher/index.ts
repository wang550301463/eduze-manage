import { api, session, errorMessage, open } from "../../core/runtime";
import type {
  Theme,
  RosterEntry,
  PortfolioRecord,
} from "../../../packages/contracts";
interface Schedule {
  days: {
    date: string;
    lessons: {
      id: string;
      startAt: string;
      classGroupName: string;
      courseName: string;
    }[];
  }[];
}
Page({
  data: {
    themes: [] as Theme[],
    lessons: [] as {
      id: string;
      startAt: string;
      classGroupName: string;
      courseName: string;
    }[],
    roster: [] as RosterEntry[],
    themeId: "",
    theme: null as Theme | null,
    error: "",
    busy: false,
  },
  onShow() {
    this.setData({
      themes: [],
      lessons: [],
      roster: [],
      theme: null,
      themeId: "",
      error: "",
    });
    void this.load();
  },
  async load() {
    if (!session.token || session.scope.role !== "TEACHER") {
      this.setData({ error: "请从首页选择老师身份" });
      return;
    }
    this.setData({ busy: true, error: "" });
    try {
      const [themes, schedule] = await Promise.all([
        api.get<Theme[]>(
          `/v1/teaching/themes?branchId=${session.scope.branchId}`,
        ),
        api.get<Schedule>(
          `/schedule/week?branchId=${session.scope.branchId}&weekStart=${new Date().toISOString().slice(0, 10)}`,
        ),
      ]);
      this.setData({
        themes,
        lessons: schedule.days.flatMap((d) => d.lessons),
      });
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async theme(e: MiniEvent) {
    const id = e.currentTarget.dataset.id;
    this.setData({
      themeId: id,
      theme: this.data.themes.find((t) => t.id === id) ?? null,
      roster: [],
      error: "",
    });
    try {
      this.setData({
        roster: await api.get<RosterEntry[]>(
          `/v1/portfolio/themes/${id}/roster`,
        ),
      });
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    }
  },
  async record(e: MiniEvent) {
    this.setData({ busy: true, error: "" });
    try {
      const studentId = e.currentTarget.dataset.id;
      const row = this.data.roster.find((r) => r.studentId === studentId);
      let id = row?.recordId;
      if (!id) {
        const record = await api.post<PortfolioRecord>(
          "/v1/portfolio/records",
          {
            themeId: this.data.themeId,
            studentId,
            progress: "IN_PROGRESS",
            classroomNote: "",
            comment: "",
            artworks: [],
            audioMediaIds: [],
          },
        );
        id = record.id;
      }
      open(`/pages/record/index?id=${id}`);
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    } finally {
      this.setData({ busy: false });
    }
  },
});
