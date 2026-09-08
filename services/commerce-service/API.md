# Commerce API
All paths begin `/api/v1/commerce`, return `{code:0,data,...}` except raw WeChat callbacks. IDs strings, money integer minor units (CNY cents), timestamps RFC3339. Staff requires `commerce:write` + branch. Public visitors can browse; order/payment need login. No fake payment mode in production.

- GET `/public/products?branchId=` -> published Product[]. Staff GET `/products?branchId=` includes unpublished.
- POST `/products` body `{id?,version,branchId,type:"COURSE"|"ACTIVITY"|"PHYSICAL",title,description,priceMinor,stock,courseId?,lessonUnits,published}` -> Product `{...input,id,version}`. stock = remaining purchasable quantity/seats; COURSE requires courseId and lessonUnits per item.
- POST `/orders` body `{branchId,productId,studentId?,quantity,idempotencyKey}` -> Order. COURSE requires authorized student. Same key same payload returns same order; changed payload409.
- GET `/orders` -> own Order[]. Staff GET `/orders?branchId=` -> branch orders. GET `/orders/{id}` -> Order.
- Order: `{id,branchId,userId,productId,studentId,courseId,productType,productTitle,unitMinor,quantity,totalMinor,lessonUnits,paymentStatus,fulfillmentStatus,trackingNumber,createdAt}`. lessonUnits total. paymentStatus UNPAID/PAID/REFUNDING/REFUNDED/CANCELLED; fulfillmentStatus PENDING/READY/ISSUED/FAILED/SHIPPED/COMPLETED/REDEEMED/REFUNDED.
- POST `/orders/{id}/payment` no body -> `{timeStamp,nonceStr,package,signType:"RSA",paySign}` for `wx.requestPayment`. Resolve openid from identity, no client openid accepted. Missing production merchant settings503. PC should display “请在小程序支付”; payment creation binds owner.
- POST `/orders/{id}/cancel` no body -> null, queries/closes provider transaction before releasing stock. Already paid409.
- POST `/orders/{id}/refunds` body `{idempotencyKey,reason}` -> Refund `{id,orderId,amountMinor,status,reason}`. Full refund v1. Used COURSE rights and shipped/redeemed items cannot auto-refund; open aftersale. Same key repeat safe.
- GET `/orders/{id}/refunds` -> Refund[]. status REQUESTED/FROZEN/SUBMIT_UNKNOWN/PROCESSING/PROVIDER_SUCCESS/PROVIDER_FAILED/PROVIDER_ABNORMAL/COMPLETED/FAILED. UNKNOWN and pending do not mean failed; ABNORMAL requires merchant-console handling and retains frozen rights.
- POST `/orders/{id}/fulfillment` staff `{status,trackingNumber?}` -> null. PHYSICAL READY->SHIPPED->COMPLETED or READY->COMPLETED (pickup), ACTIVITY READY->REDEEMED; COURSE rights issued by academic only.
- POST `/orders/{id}/aftersales` body `{reason}` -> `{id}`. GET same -> `{id,reason,status,response,createdAt}`[].
- POST `/orders/{id}/aftersales/{requestId}/resolve` staff `{status:"PROCESSING"|"CLOSED",response}` -> null.
- GET `/dashboard?branchId=` -> `{paidMinor,refundedMinor,netReceiptsMinor,definition}`. Net receipts = paid minus completed refund, not profit.
- POST `/callbacks/payment` and `/callbacks/refund`: provider raw signed body/header only; success `{code:"SUCCESS",message:"成功"}`.

COURSE inventory is offer availability, never a class seat. ACTIVITY stock is paid ticket availability with staff redemption. Shipping tracking, persisted recipient/contact/address and pickup are supported.

Delivery addition: OrderInput accepts `fulfillmentMethod:"PICKUP"|"DELIVERY"` (default PICKUP), `recipientName`, `recipientPhone`, `address`; Order returns all four. Only PHYSICAL goods support DELIVERY, and all contact/address fields then required. The address is persisted with the order and included in idempotency comparison. SHIPPED requires DELIVERY; PICKUP uses READY->COMPLETED.

Operations (staff only):
- GET `/reconciliations?branchId=` -> `{id,orderId,providerStatus,result,checkedAt}`[].
- POST `/orders/{id}/reconcile` -> current Order after verified provider lookup; never accepts client paid status.
- GET `/refunds/pending?branchId=` -> `{id,orderId,reason,amountMinor,status,lastError,attempts,updatedAt}`[].
- POST `/orders/{id}/refunds/{refundId}/retry` -> Refund. Requeries provider, keeps the same refund identifier; abnormal refund must first be resolved in merchant console.

Review corrections:
- `ProductInput.version`: create uses 0; update requires the fetched Product.version. Every order reservation, cancellation, refund stock return and product edit increments this version. A stale edit returns409 without changing stock; refresh before editing again. Stock remains the available quantity.
- Academic freeze404 (grant not delivered yet) and403 (authorization/manual verification) keep the existing refund REQUESTED and order REFUNDING; no provider refund is submitted. The same refund ID is retried. Pending-refund operations display the reason. A genuinely rejected grant must first have its academic rights/authorization repaired or manually verified before retry; this version does not automatically refund an unissued order without an academic freeze/tombstone. Actual consumed-rights409 rejects the refund without spending money.
