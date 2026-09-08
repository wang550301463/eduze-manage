import { api, session, errorMessage, open } from "../../core/runtime";
import {
  money,
  type Product,
  type Studio,
  type Order,
} from "../../../packages/contracts/business";
import type { Child } from "../../../packages/contracts";
Page({
  data: {
    studios: [] as Studio[],
    branchId: "",
    branchName: "选择画室",
    products: [] as (Product & { priceText: string })[],
    selected: null as (Product & { priceText: string }) | null,
    children: [] as Child[],
    childId: "",
    childName: "选择使用课程的孩子",
    quantity: "1",
    fulfillmentMethod: "PICKUP",
    recipientName: "",
    recipientPhone: "",
    address: "",
    requestKey: "",
    busy: false,
    error: "",
    success: "",
  },
  onLoad(options: { branchId?: string }) {
    void this.load(options.branchId);
  },
  onShow() {
    this.setData({
      selected: null,
      children: [],
      childId: "",
      recipientName: "",
      recipientPhone: "",
      address: "",
      error: "",
    });
    if (session.token) void this.loadChildren();
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
  async loadChildren() {
    try {
      this.setData({
        children: (
          await api.get<Child[]>("/v1/academic/family/children")
        ).filter(
          (c) => !this.data.branchId || c.branchId === this.data.branchId,
        ),
      });
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    }
  },
  async selectStudio(studio: Studio) {
    session.switchScope({
      branchId: studio.branchId,
      childId:
        session.scope.branchId === studio.branchId ? session.scope.childId : "",
    });
    this.setData({
      branchId: studio.branchId,
      branchName: studio.name,
      products: [],
      selected: null,
      childId: "",
      childName: "选择使用课程的孩子",
      error: "",
    });
    if (session.token) await this.loadChildren();
    const products = await api.get<Product[]>(
      `/v1/commerce/public/products?branchId=${studio.branchId}`,
    );
    this.setData({
      products: products.map((p) => ({ ...p, priceText: money(p.priceMinor) })),
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
  select(e: MiniEvent) {
    this.setData({
      selected:
        this.data.products.find((p) => p.id === e.currentTarget.dataset.id) ??
        null,
      requestKey: `order-${Date.now()}-${Math.random().toString(36).slice(2)}`,
      quantity: "1",
      fulfillmentMethod: "PICKUP",
      recipientName: "",
      recipientPhone: "",
      address: "",
      error: "",
    });
  },
  field(e: MiniEvent) {
    this.setData({
      [e.currentTarget.dataset.field]: e.detail.value,
      requestKey: `order-${Date.now()}-${Math.random().toString(36).slice(2)}`,
    });
  },
  child(e: MiniEvent) {
    const child = this.data.children[Number(e.detail.value)];
    if (child)
      this.setData({
        childId: child.id,
        childName: child.name,
        requestKey: `order-${Date.now()}-${Math.random().toString(36).slice(2)}`,
      });
  },
  method(e: MiniEvent) {
    this.setData({
      fulfillmentMethod: e.currentTarget.dataset.method,
      requestKey: `order-${Date.now()}-${Math.random().toString(36).slice(2)}`,
    });
  },
  async order() {
    if (!session.token) {
      this.setData({ error: "请先在首页登录后下单" });
      return;
    }
    const p = this.data.selected;
    if (!p) return;
    if (p.type === "COURSE" && !this.data.childId) {
      this.setData({ error: "请先选择已授权的孩子" });
      return;
    }
    const quantity = Number(this.data.quantity);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 100) {
      this.setData({ error: "购买数量须为 1 至 100 的整数" });
      return;
    }
    if (
      p.type === "PHYSICAL" &&
      this.data.fulfillmentMethod === "DELIVERY" &&
      (!this.data.recipientName.trim() ||
        !this.data.recipientPhone.trim() ||
        !this.data.address.trim())
    ) {
      this.setData({ error: "请填写收件人、联系电话和完整配送地址" });
      return;
    }
    this.setData({ busy: true, error: "" });
    try {
      const order = await api.post<Order>("/v1/commerce/orders", {
        branchId: p.branchId,
        productId: p.id,
        studentId: p.type === "COURSE" ? this.data.childId : null,
        quantity: Number(this.data.quantity),
        idempotencyKey: this.data.requestKey,
        fulfillmentMethod:
          p.type === "PHYSICAL" ? this.data.fulfillmentMethod : "PICKUP",
        recipientName:
          this.data.fulfillmentMethod === "DELIVERY"
            ? this.data.recipientName
            : null,
        recipientPhone:
          this.data.fulfillmentMethod === "DELIVERY"
            ? this.data.recipientPhone
            : null,
        address:
          this.data.fulfillmentMethod === "DELIVERY" ? this.data.address : null,
      });
      this.setData({ selected: null });
      open(`/pages/orders/index?id=${order.id}`);
    } catch (error) {
      this.setData({ error: errorMessage(error) });
    } finally {
      this.setData({ busy: false });
    }
  },
  orders() {
    open("/pages/orders/index");
  },
});
