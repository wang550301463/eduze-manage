import type { Order, WxPayment } from "../../packages/contracts/business";
type RequestClient = {
  post(path: string, data: unknown): Promise<unknown>;
  get(path: string): Promise<unknown>;
};
export async function payAndRefresh(
  orderId: string,
  client: RequestClient,
  launch: (params: WxPayment) => Promise<void>,
): Promise<{ order: Order; notice: string }> {
  const params = (await client.post(
    `/v1/commerce/orders/${orderId}/payment`,
    {},
  )) as WxPayment;
  if (
    !params ||
    !params.timeStamp ||
    !params.nonceStr ||
    !params.package?.startsWith("prepay_id=") ||
    params.signType !== "RSA" ||
    !params.paySign
  )
    throw new Error("支付参数不完整，请重新打开订单后重试。");
  let interrupted = false;
  try {
    await launch(params);
  } catch {
    interrupted = true;
  }
  const order = (await client.get(`/v1/commerce/orders/${orderId}`)) as Order;
  return {
    order,
    notice:
      order.paymentStatus === "PAID"
        ? "支付已由服务端确认，请继续查看交付状态。"
        : interrupted
          ? "支付未完成，订单状态已重新查询。"
          : "微信已返回，请刷新订单等待服务端确认。",
  };
}
