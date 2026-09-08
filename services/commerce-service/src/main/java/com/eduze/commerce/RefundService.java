package com.eduze.commerce;

import static com.eduze.commerce.CommerceModels.*;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Full-refund saga. All remote retries retain one immutable refund identifier. */
@Service
public class RefundService {
    private final JdbcTemplate jdbc;
    private final OrderService orders;
    private final PaymentGateway gateway;
    private final InternalClient client;
    private final TransactionTemplate transactions;

    public RefundService(
            JdbcTemplate jdbc,
            OrderService orders,
            PaymentGateway gateway,
            InternalClient client,
            PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.gateway = gateway;
        this.client = client;
        this.transactions = new TransactionTemplate(manager);
    }

    public Refund request(Actor actor, String orderId, RefundInput input) {
        CatalogService.text(input.idempotencyKey(), 100);
        CatalogService.text(input.reason(), 500);
        String id =
                transactions.execute(
                        status -> {
                            jdbc.queryForList(
                                    "SELECT id FROM commerce_order WHERE id=? FOR UPDATE", orderId);
                            var order = orders.get(actor, orderId);
                            var prior =
                                    jdbc.queryForList(
                                            "SELECT id,idempotency_key,reason FROM commerce_refund WHERE order_id=?",
                                            orderId);
                            if (!prior.isEmpty()) {
                                if (!input.idempotencyKey()
                                                .equals(prior.get(0).get("idempotency_key"))
                                        || !input.reason().equals(prior.get(0).get("reason"))) {
                                    throw new PlatformException(409, "订单已存在退款申请，幂等键或原因不符");
                                }
                                return (String) prior.get(0).get("id");
                            }
                            if (!"PAID".equals(order.paymentStatus())
                                    || Set.of("SHIPPED", "COMPLETED", "REDEEMED")
                                            .contains(order.fulfillmentStatus())) {
                                throw new PlatformException(409, "订单已履约，请通过售后处理");
                            }
                            String refund = OrderService.id();
                            jdbc.update(
                                    "INSERT INTO commerce_refund(id,order_id,tenant_id,idempotency_key,reason,amount_minor,status) VALUES(?,?,?,?,?,?,'REQUESTED')",
                                    refund,
                                    orderId,
                                    actor.tenantId(),
                                    input.idempotencyKey(),
                                    input.reason(),
                                    order.totalMinor());
                            jdbc.update(
                                    "UPDATE commerce_order SET payment_status='REFUNDING',updated_at=CURRENT_TIMESTAMP WHERE id=?",
                                    orderId);
                            return refund;
                        });
        try {
            advance(id);
        } catch (PlatformException error) {
            failedAttempt(id, error.getStatus());
        }
        return get(id);
    }

    public List<Refund> list(Actor actor, String orderId) {
        orders.get(actor, orderId);
        return jdbc.query(
                "SELECT id,order_id,amount_minor,status,reason FROM commerce_refund WHERE order_id=?",
                (r, n) ->
                        new Refund(
                                r.getString(1),
                                r.getString(2),
                                r.getLong(3),
                                r.getString(4),
                                r.getString(5)),
                orderId);
    }

    Refund get(String id) {
        return jdbc.queryForObject(
                "SELECT id,order_id,amount_minor,status,reason FROM commerce_refund WHERE id=?",
                (r, n) ->
                        new Refund(
                                r.getString(1),
                                r.getString(2),
                                r.getLong(3),
                                r.getString(4),
                                r.getString(5)),
                id);
    }

    private Map<String, Object> context(String id) {
        var rows =
                jdbc.queryForList(
                        "SELECT r.id,r.order_id,r.status,r.reason,r.amount_minor,o.tenant_id,o.branch_id,o.student_id,o.lesson_units,o.product_type,o.product_id,o.quantity,o.transaction_id FROM commerce_refund r JOIN commerce_order o ON o.id=r.order_id WHERE r.id=?",
                        id);
        if (rows.isEmpty()) {
            throw new PlatformException(404, "退款不存在");
        }
        return rows.get(0);
    }

    private Map<String, Object> rights(Map<String, Object> r) {
        return Map.of(
                "refundId",
                r.get("id"),
                "orderId",
                r.get("order_id"),
                "tenantId",
                r.get("tenant_id"),
                "branchId",
                r.get("branch_id"),
                "studentId",
                r.get("student_id"),
                "lessonUnits",
                r.get("lesson_units"));
    }

    public void advance(String id) {
        Map<String, Object> r = context(id);
        String status = (String) r.get("status");
        if (Set.of("COMPLETED", "FAILED").contains(status)) {
            return;
        }
        boolean course = "COURSE".equals(r.get("product_type"));
        if ("REQUESTED".equals(status)) {
            if (course) {
                Map<?, ?> result;
                try {
                    result =
                            client.post(
                                    "academic",
                                    "/internal/academic/commerce/refunds/freeze",
                                    rights(r),
                                    Map.class);
                } catch (PlatformException error) {
                    if (error.getStatus() == 409) {
                        String failedOrder = (String) r.get("order_id");
                        transactions.execute(
                                s -> {
                                    jdbc.update(
                                            "UPDATE commerce_refund SET status='FAILED',last_error='可退权益不足或不可访问' WHERE id=? AND status='REQUESTED'",
                                            id);
                                    jdbc.update(
                                            "UPDATE commerce_order SET payment_status='PAID' WHERE id=? AND payment_status='REFUNDING'",
                                            failedOrder);
                                    return null;
                                });
                        return;
                    }
                    throw error;
                }
                if (result == null || !"FROZEN".equals(result.get("status"))) {
                    throw new PlatformException(503, "退款权益尚未冻结");
                }
            }
            jdbc.update(
                    "UPDATE commerce_refund SET status='FROZEN',updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='REQUESTED'",
                    id);
            status = "FROZEN";
        }
        if (Set.of("FROZEN", "SUBMIT_UNKNOWN", "PROCESSING").contains(status)) {
            // Query before every submission, including recovery after network timeout or process
            // crash.
            JsonNode result = gateway.queryRefund(id);
            if ("NOT_FOUND".equals(result.path("status").asText())) {
                jdbc.update(
                        "UPDATE commerce_refund SET status='SUBMIT_UNKNOWN',updated_at=CURRENT_TIMESTAMP WHERE id=? AND status IN ('FROZEN','SUBMIT_UNKNOWN','PROCESSING')",
                        id);
                result =
                        gateway.refund(
                                (String) r.get("order_id"),
                                id,
                                ((Number) r.get("amount_minor")).longValue(),
                                (String) r.get("reason"));
            }
            providerResult(result, false);
            r = context(id);
            status = (String) r.get("status");
        }
        if ("PROVIDER_SUCCESS".equals(status)) {
            if (course) {
                Map<?, ?> result =
                        client.post(
                                "academic",
                                "/internal/academic/commerce/refunds/complete",
                                rights(r),
                                Map.class);
                if (result == null || !"COMPLETED".equals(result.get("status"))) {
                    throw new PlatformException(503, "退款权益冲正待处理");
                }
            }
            final Map<String, Object> completed = r;
            transactions.execute(
                    s -> {
                        if (jdbc.update(
                                        "UPDATE commerce_refund SET status='COMPLETED',last_error=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PROVIDER_SUCCESS'",
                                        id)
                                == 1) {
                            jdbc.update(
                                    "UPDATE commerce_order SET payment_status='REFUNDED',fulfillment_status='REFUNDED',updated_at=CURRENT_TIMESTAMP WHERE id=?",
                                    completed.get("order_id"));
                            jdbc.update(
                                    "UPDATE product SET stock=stock+?,version=version+1 WHERE id=?",
                                    completed.get("quantity"),
                                    completed.get("product_id"));
                        }
                        return null;
                    });
        } else if ("PROVIDER_FAILED".equals(status)) {
            if (course) {
                Map<?, ?> result =
                        client.post(
                                "academic",
                                "/internal/academic/commerce/refunds/release",
                                rights(r),
                                Map.class);
                if (result == null || !"RELEASED".equals(result.get("status"))) {
                    throw new PlatformException(503, "退款冻结释放待处理");
                }
            }
            final Map<String, Object> failed = r;
            transactions.execute(
                    s -> {
                        jdbc.update(
                                "UPDATE commerce_refund SET status='FAILED',updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PROVIDER_FAILED'",
                                id);
                        jdbc.update(
                                "UPDATE commerce_order SET payment_status='PAID',updated_at=CURRENT_TIMESTAMP WHERE id=? AND payment_status='REFUNDING'",
                                failed.get("order_id"));
                        return null;
                    });
        }
    }

    public void providerResult(JsonNode body, boolean callback) {
        if (callback) {
            gateway.verifyMerchant(body, false);
        }
        String id = body.path("out_refund_no").asText();
        Map<String, Object> r = context(id);
        if (!r.get("order_id").equals(body.path("out_trade_no").asText())
                || body.path("amount").path("refund").asLong(-1)
                        != ((Number) r.get("amount_minor")).longValue()
                || body.path("amount").path("total").asLong(-1)
                        != ((Number) r.get("amount_minor")).longValue()
                || ((!callback || body.path("amount").has("currency"))
                        && !"CNY".equals(body.path("amount").path("currency").asText()))
                || !r.get("transaction_id").equals(body.path("transaction_id").asText())) {
            throw new PlatformException(409, "退款订单或金额不符");
        }
        String provider = body.path(callback ? "refund_status" : "status").asText();
        String status =
                switch (provider) {
                    case "SUCCESS" -> "PROVIDER_SUCCESS";
                    case "CLOSED" -> "PROVIDER_FAILED";
                    case "ABNORMAL" -> "PROVIDER_ABNORMAL";
                    case "PROCESSING" -> "PROCESSING";
                    default -> throw new PlatformException(400, "未知退款状态");
                };
        jdbc.update(
                "UPDATE commerce_refund SET status=?,provider_refund_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status NOT IN ('COMPLETED','FAILED','PROVIDER_SUCCESS','PROVIDER_FAILED')",
                status,
                body.path("refund_id").asText(),
                id);
    }

    void failedAttempt(String id, int errorCode) {
        jdbc.update(
                "UPDATE commerce_refund SET attempts=attempts+1,last_error=?,next_attempt_at=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                (errorCode == 404
                        ? "等待教务权益发放或修复拒发记录；核实后重试，尚未发起资金退款"
                        : errorCode == 403
                                ? "教务权益访问被拒绝，请核实授权后重试；尚未发起资金退款"
                                : "外部处理待核对，错误码 " + errorCode),
                java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(60)),
                id);
    }
}
