package com.eduze.commerce;

import com.eduze.platform.runtime.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EntitlementHandler implements EventHandler {
    private final JdbcTemplate jdbc;

    public EntitlementHandler(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean supports(String type) {
        return "academic.entitlement-issued".equals(type)
                || "academic.entitlement-rejected".equals(type);
    }

    @Override
    public void handle(EventEnvelope event) {
        String id = event.payload().path("orderId").asText();
        var rows =
                jdbc.queryForList(
                        "SELECT id FROM commerce_order WHERE id=? AND tenant_id=? AND branch_id=? AND product_type='COURSE'",
                        id,
                        event.tenantId(),
                        event.branchId());
        if (rows.isEmpty()) {
            throw new PlatformException(404, "课程订单不存在");
        }
        jdbc.update(
                "UPDATE commerce_order SET fulfillment_status=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND payment_status IN ('PAID','REFUNDING') AND fulfillment_status IN ('PENDING','FAILED')",
                "academic.entitlement-issued".equals(event.type()) ? "ISSUED" : "FAILED",
                id);
    }
}
