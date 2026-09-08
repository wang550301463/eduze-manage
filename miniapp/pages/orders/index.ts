import { api, session, errorMessage } from "../../core/runtime";
import { payAndRefresh } from "../../core/payment";
import {
  money,
  paymentLabels,
  fulfillmentLabels,
  refundLabels,
  type Order,
  type Refund,
  type Aftersale,
} from "../../../packages/contracts/business";
type OrderView = Order & {
  totalText: string;
  paymentLabel: string;
  fulfillmentLabel: string;
};
const view = (o: Order): OrderView => ({
  ...o,
  totalText: money(o.totalMinor),
  paymentLabel: paymentLabels[o.paymentStatus] ?? o.paymentStatus,
  fulfillmentLabel:
    fulfillmentLabels[o.fulfillmentStatus] ?? o.fulfillmentStatus,
});
Page({
  data: {
    orders: [] as OrderView[],
    selected: null as OrderView | null,
    refunds: [] as (Refund & { amountText: string; statusLabel: string })[],
    aftersales: [] as Aftersale[],
    reason: "",
    busy: false,
    error: "",
    success: "",
    initialId: "",
  },
  onLoad(options: { id?: string }) {
    this.setData({ initialId: options.id ?? "" });
  },
  onShow() {
    this.setData({
      orders: [],
      selected: null,
      refunds: [],
      aftersales: [],
      reason: "",
      error: "",
      success: "",
    });
    void this.load();
  },
  async load() {
    if (!session.token) {
      this.setData({ error: "请先从首页登录" });
      return;
    }
    this.setData({ busy: true, error: "" });
    try {
      const orders = await api.get<Order[]>("/v1/commerce/orders");
      this.setData({ orders: orders.map(view) });
      if (this.data.initialId) {
        await this.detail(this.data.initialId);
        this.setData({ initialId: "" });
      }
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async detail(id: string) {
    const [order, refunds, aftersales] = await Promise.all([
      api.get<Order>(`/v1/commerce/orders/${id}`),
      api.get<Refund[]>(`/v1/commerce/orders/${id}/refunds`),
      api.get<Aftersale[]>(`/v1/commerce/orders/${id}/aftersales`),
    ]);
    this.setData({
      selected: view(order),
      refunds: refunds.map((r) => ({
        ...r,
        amountText: money(r.amountMinor),
        statusLabel: refundLabels[r.status] ?? r.status,
      })),
      aftersales,
    });
  },
  async select(e: MiniEvent) {
    this.setData({
      busy: true,
      error: "",
      reason: "",
      selected: null,
      refunds: [],
      aftersales: [],
    });
    try {
      await this.detail(e.currentTarget.dataset.id);
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  reason(e: MiniEvent) {
    this.setData({ reason: e.detail.value });
  },
  async pay() {
    const o = this.data.selected;
    if (!o) return;
    this.setData({ busy: true, error: "", success: "" });
    try {
      const result = await payAndRefresh(
        o.id,
        api,
        (params) =>
          new Promise<void>((resolve, reject) =>
            wx.requestPayment({ ...params, success: resolve, fail: reject }),
          ),
      );
      this.setData({ selected: view(result.order), success: result.notice });
      await this.load();
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async refresh() {
    const id = this.data.selected?.id;
    if (!id) return;
    this.setData({ busy: true, error: "" });
    try {
      await this.detail(id);
      await this.load();
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async cancel() {
    const o = this.data.selected;
    if (!o) return;
    this.setData({ busy: true, error: "", success: "" });
    try {
      await api.post(`/v1/commerce/orders/${o.id}/cancel`, {});
      await this.detail(o.id);
      await this.load();
      this.setData({ success: "订单已取消" });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async refund() {
    const o = this.data.selected;
    if (!o) return;
    this.setData({ busy: true, error: "", success: "" });
    try {
      await api.post(`/v1/commerce/orders/${o.id}/refunds`, {
        reason: this.data.reason,
        idempotencyKey: `refund-${o.id}`,
      });
      await this.detail(o.id);
      this.setData({ success: "退款申请已提交，完成时间以退款进度为准。" });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  async aftersale() {
    const o = this.data.selected;
    if (!o) return;
    this.setData({ busy: true, error: "", success: "" });
    try {
      await api.post(`/v1/commerce/orders/${o.id}/aftersales`, {
        reason: this.data.reason,
      });
      await this.detail(o.id);
      this.setData({ success: "售后已提交，画室会在处理后回复。", reason: "" });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
});
