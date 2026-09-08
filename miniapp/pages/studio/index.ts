import { api, session, errorMessage, open } from "../../core/runtime";
import type {
  Studio,
  Activity,
  Signup,
} from "../../../packages/contracts/business";
interface SavedSignup extends Signup {
  activityTitle: string;
  startsAt: string;
}
Page({
  data: {
    studios: [] as Studio[],
    studio: null as Studio | null,
    activities: [] as Activity[],
    signups: [] as SavedSignup[],
    studentName: "",
    phone: "",
    age: "6",
    source: "MINIAPP",
    sourceId: "",
    referrerId: "",
    busy: false,
    error: "",
    success: "",
    intent: "咨询课程",
  },
  onLoad(options: {
    branchId?: string;
    source?: string;
    sourceId?: string;
    referrerId?: string;
  }) {
    this.setData({
      source: options.source ?? "MINIAPP",
      sourceId: options.sourceId ?? "",
      referrerId: options.referrerId ?? "",
    });
    void this.load(options.branchId);
  },
  onShow() {
    this.setData({ studentName: "", phone: "", signups: [], error: "" });
    if (session.token) void this.loadSignups();
  },
  async load(branchId?: string) {
    this.setData({ busy: true, error: "" });
    try {
      const studios = await api.get<Studio[]>("/v1/engagement/public/studios");
      this.setData({ studios });
      const studio =
        studios.find(
          (s) => s.branchId === (branchId ?? session.scope.branchId),
        ) ?? studios[0];
      if (studio) await this.selectStudio(studio);
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async selectStudio(studio: Studio) {
    session.switchScope({
      branchId: studio.branchId,
      childId:
        session.scope.branchId === studio.branchId ? session.scope.childId : "",
    });
    this.setData({ studio, activities: [], error: "", success: "" });
    this.setData({
      activities: await api.get<Activity[]>(
        `/v1/engagement/public/activities?branchId=${studio.branchId}`,
      ),
    });
  },
  async branch(e: MiniEvent) {
    try {
      const studio = this.data.studios[Number(e.detail.value)];
      if (studio) await this.selectStudio(studio);
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    }
  },
  field(e: MiniEvent) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value });
  },
  intent(e: MiniEvent) {
    this.setData({ intent: e.currentTarget.dataset.intent });
  },
  async enquire() {
    if (!this.data.studio) return;
    this.setData({ busy: true, error: "", success: "" });
    try {
      await api.post(
        `/v1/engagement/public/studios/${this.data.studio.branchId}/enquiries`,
        {
          studentName: this.data.studentName,
          phone: this.data.phone,
          age: Number(this.data.age),
          source:
            this.data.intent === "预约试听" ? "TRIAL_INTENT" : this.data.source,
          sourceId: this.data.sourceId,
          referrerId: this.data.referrerId,
          website: "",
        },
      );
      this.setData({
        studentName: "",
        phone: "",
        success:
          this.data.intent === "预约试听"
            ? "试听意向已提交，画室将在确认可用课堂后联系您。"
            : "咨询已提交，画室会联系您。",
      });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async signup(e: MiniEvent) {
    if (!session.token) {
      this.setData({ error: "请先在首页登录，再报名活动" });
      return;
    }
    this.setData({ busy: true, error: "", success: "" });
    try {
      await api.post(
        `/v1/engagement/activities/${e.currentTarget.dataset.id}/signups`,
        {},
      );
      await this.loadSignups();
      if (this.data.studio) await this.selectStudio(this.data.studio);
      this.setData({ success: "活动报名已确认，可在下方查看报名记录" });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async loadSignups() {
    try {
      this.setData({
        signups: await api.get<SavedSignup[]>("/v1/engagement/signups"),
      });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    }
  },
  async cancel(e: MiniEvent) {
    this.setData({ busy: true, error: "" });
    try {
      await api.post(
        `/v1/engagement/signups/${e.currentTarget.dataset.id}/cancel`,
        {},
      );
      await this.loadSignups();
      this.setData({ success: "报名已取消" });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  phone() {
    if (this.data.studio?.phone)
      wx.makePhoneCall({ phoneNumber: this.data.studio.phone });
  },
  shop() {
    open(`/pages/shop/index?branchId=${this.data.studio?.branchId ?? ""}`);
  },
  onShareAppMessage() {
    return {
      title: this.data.studio?.name ?? "一起走进画室",
      path: `/pages/studio/index?branchId=${this.data.studio?.branchId ?? ""}&source=REFERRAL&referrerId=${session.user?.id ?? ""}`,
    };
  },
});
