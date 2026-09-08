import type { NotificationMessage as Message } from "../../../packages/contracts";
import { api, session, errorMessage, open } from "../../core/runtime";
import { config } from "../../config";

Page({
  data: { messages: [] as Message[], error: "", busy: false, success: "" },
  onShow() {
    this.setData({ messages: [], error: "", success: "" });
    void this.load();
  },
  async load() {
    if (!session.token) {
      this.setData({ error: "请先登录" });
      return;
    }
    try {
      this.setData({ messages: await api.get<Message[]>("/v1/notifications") });
    } catch (e) {
      this.setData({ error: errorMessage(e) });
    }
  },
  async read(e: MiniEvent) {
    this.setData({ busy: true, error: "" });
    try {
      await api.post(
        `/v1/notifications/${e.currentTarget.dataset.id}/read`,
        {},
      );
      await this.load();
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async confirm(e: MiniEvent) {
    this.setData({ busy: true, error: "" });
    try {
      await api.post(
        `/v1/notifications/${e.currentTarget.dataset.id}/confirm`,
        {},
      );
      await this.load();
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  business(e: MiniEvent) {
    const m = this.data.messages.find(
      (m) => m.id === e.currentTarget.dataset.id,
    );
    const route = m?.path?.replace(/^pages\//, "/pages/");
    if (
      route &&
      /^\/pages\/(family|teacher|record|exhibitions)\/index(?:\?|$)/.test(route)
    )
      open(route);
    else if (session.scope.role === "TEACHER") open("/pages/teacher/index");
    else if (session.scope.childId) open("/pages/family/index");
    else open("/pages/home/index");
  },
  subscribe() {
    const keys = Object.keys(config.templateIds);
    if (!keys.length) {
      this.setData({ error: "画室尚未配置微信通知模板，请先查看站内消息" });
      return;
    }
    const tmplIds = keys.slice(0, 3).map((k) => config.templateIds[k]);
    wx.requestSubscribeMessage({
      tmplIds,
      success: (result) => {
        void Promise.all(
          keys.slice(0, 3).map((templateKey) =>
            api.post(
              "/v1/notifications/subscriptions",
              {
                templateKey,
                accepted: result[config.templateIds[templateKey]] === "accept",
              },
              "PUT",
            ),
          ),
        )
          .then(() =>
            this.setData({
              success: "订阅选择已保存，实际发送以微信授权结果为准",
            }),
          )
          .catch((e) => this.setData({ error: errorMessage(e) }));
      },
      fail: (e) => this.setData({ error: e.errMsg }),
    });
  },
});
