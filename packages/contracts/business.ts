import type { components as Engagement } from "./generated/engagement";
import type { components as Commerce } from "./generated/commerce";
/** Required fields describe populated responses; nullable SQL projections and state unions are explicit client refinements. */
export type Studio = Required<Engagement["schemas"]["Studio"]>;
export type Enquiry = Omit<
  Required<Engagement["schemas"]["Enquiry"]>,
  "reservationId"
> & { reservationId: string | null };
export type Followup = Omit<
  Engagement["schemas"]["FollowupInput"],
  "nextAt"
> & { authorId: string; createdAt: string; nextAt: string | null };
export type Activity = Required<Engagement["schemas"]["Activity"]>;
export type Signup = Required<Engagement["schemas"]["Signup"]>;
export type Renewal = Omit<
  Engagement["schemas"]["RenewalInput"],
  "branchId" | "nextAt" | "status"
> & {
  id: string;
  nextAt: string | null;
  status: "OPEN" | "CONTACTED" | "CLOSED";
};
export type ProductInput = Required<
  Omit<Commerce["schemas"]["ProductInput"], "id" | "courseId" | "type">
> &
  Pick<Commerce["schemas"]["ProductInput"], "id" | "courseId"> & {
    type: "COURSE" | "ACTIVITY" | "PHYSICAL";
  };
export type Product = Omit<
  Required<Commerce["schemas"]["Product"]>,
  "type" | "courseId"
> & { type: ProductInput["type"]; courseId?: string };

export type Order = Omit<
  Required<Commerce["schemas"]["Order"]>,
  | "fulfillmentMethod"
  | "recipientName"
  | "recipientPhone"
  | "address"
  | "studentId"
  | "courseId"
  | "trackingNumber"
  | "productType"
  | "paymentStatus"
  | "fulfillmentStatus"
> & {
  fulfillmentMethod?: "PICKUP" | "DELIVERY";
  recipientName?: string;
  recipientPhone?: string;
  address?: string;
  studentId: string | null;
  courseId: string | null;
  trackingNumber: string | null;
  productType: Product["type"];
  paymentStatus: "UNPAID" | "PAID" | "REFUNDING" | "REFUNDED" | "CANCELLED";
  fulfillmentStatus:
    | "PENDING"
    | "READY"
    | "ISSUED"
    | "FAILED"
    | "SHIPPED"
    | "COMPLETED"
    | "REDEEMED"
    | "REFUNDED";
};

export type Refund = Required<Commerce["schemas"]["Refund"]>;
export interface Aftersale {
  id: string;
  reason: string;
  status: "OPEN" | "PROCESSING" | "CLOSED";
  response: string | null;
  createdAt: string;
}
export interface Revenue {
  paidMinor: number;
  refundedMinor: number;
  netReceiptsMinor: number;
  definition: string;
}
export interface WxPayment {
  timeStamp: string;
  nonceStr: string;
  package: string;
  signType: "RSA";
  paySign: string;
}
export const paymentLabels: Record<string, string> = {
  UNPAID: "待支付",
  PAID: "支付成功",
  REFUNDING: "退款处理中",
  REFUNDED: "退款完成",
  CANCELLED: "已取消",
};
export const fulfillmentLabels: Record<string, string> = {
  PENDING: "等待交付",
  READY: "待交付",
  ISSUED: "课时已发放",
  FAILED: "交付待处理",
  SHIPPED: "已发货",
  COMPLETED: "已完成",
  REDEEMED: "已核销",
  REFUNDED: "已退还",
};
export const refundLabels: Record<string, string> = {
  REQUESTED: "申请已收到",
  FROZEN: "权益已冻结",
  SUBMIT_UNKNOWN: "正在核实退款结果",
  PROCESSING: "微信处理中",
  PROVIDER_SUCCESS: "退款成功，正在完成权益处理",
  PROVIDER_FAILED: "微信退款失败",
  PROVIDER_ABNORMAL: "退款异常，等待商户处理",
  COMPLETED: "退款已完成",
  FAILED: "退款失败",
};
export function money(minor: number): string {
  return (minor / 100).toFixed(2);
}
export function parsePrice(value: string): number {
  if (!/^\d+(\.\d{1,2})?$/.test(value.trim()))
    throw new Error("金额必须为正数，最多保留两位小数");
  const [whole, fraction = ""] = value.trim().split(".");
  const minor = Number(whole) * 100 + Number(fraction.padEnd(2, "0"));
  if (!Number.isSafeInteger(minor) || minor < 1 || minor > 100000000)
    throw new Error("金额需在 0.01 至 1000000 元之间");
  return minor;
}
