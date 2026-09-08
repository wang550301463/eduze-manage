package com.eduze.commerce;

import com.eduze.platform.runtime.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CommerceOperations {
    private final JdbcTemplate jdbc;
    private final OrderService orders;
    private final PaymentService payments;
    private final PaymentGateway gateway;
    private final RefundService refunds;

    public CommerceOperations(
            JdbcTemplate jdbc,
            OrderService orders,
            PaymentService payments,
            PaymentGateway gateway,
            RefundService refunds) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.payments = payments;
        this.gateway = gateway;
        this.refunds = refunds;
    }

    public List<Map<String, Object>> reconciliations(Actor actor, String branch) {
        CatalogService.staff(actor, branch);
        return jdbc.queryForList(
                "SELECT r.id,r.order_id AS orderId,r.provider_status AS providerStatus,r.result,r.checked_at AS checkedAt FROM commerce_reconciliation r JOIN commerce_order o ON o.id=r.order_id WHERE o.tenant_id=? AND o.branch_id=? ORDER BY r.checked_at DESC LIMIT 200",
                actor.tenantId(),
                branch);
    }

    public CommerceModels.Order reconcile(Actor actor, String id) {
        var order = orders.get(actor, id);
        CatalogService.staff(actor, order.branchId());
        var body = gateway.queryOrder(id);
        String provider = body.path("trade_state").asText(body.path("status").asText());
        if ("SUCCESS".equals(provider)) {
            payments.paid(body);
        }
        jdbc.update(
                "INSERT INTO commerce_reconciliation(id,order_id,provider_status,result) VALUES(?,?,?,?)",
                OrderService.id(),
                id,
                provider,
                "SUCCESS".equals(provider) ? "MATCHED" : "REVIEW");
        return orders.get(actor, id);
    }

    public List<Map<String, Object>> pendingRefunds(Actor actor, String branch) {
        CatalogService.staff(actor, branch);
        return jdbc.queryForList(
                "SELECT r.id,r.order_id AS orderId,r.reason,r.amount_minor AS amountMinor,r.status,r.last_error AS lastError,r.attempts,r.updated_at AS updatedAt FROM commerce_refund r JOIN commerce_order o ON o.id=r.order_id WHERE o.tenant_id=? AND o.branch_id=? AND r.status NOT IN ('COMPLETED','FAILED') ORDER BY r.updated_at LIMIT 200",
                actor.tenantId(),
                branch);
    }

    public CommerceModels.Refund retryRefund(Actor actor, String orderId, String refundId) {
        var order = orders.get(actor, orderId);
        CatalogService.staff(actor, order.branchId());
        var refund = refunds.get(refundId);
        if (!orderId.equals(refund.orderId())) {
            throw new PlatformException(404, "退款不存在");
        }
        if ("PROVIDER_ABNORMAL".equals(refund.status())) {
            refunds.providerResult(gateway.queryRefund(refundId), false);
        }
        jdbc.update(
                "UPDATE commerce_refund SET attempts=0,last_error=NULL,next_attempt_at=CURRENT_TIMESTAMP WHERE id=?",
                refundId);
        refunds.advance(refundId);
        return refunds.get(refundId);
    }
}
