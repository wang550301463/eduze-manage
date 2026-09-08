package com.eduze.commerce;

import com.eduze.platform.runtime.PlatformException;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Bounded recoveries leave persisted evidence for manual follow-up after twenty failures. */
@Component
public class CommerceRecovery {
    private final JdbcTemplate jdbc;
    private final RefundService refunds;
    private final PaymentGateway gateway;
    private final PaymentService payments;

    public CommerceRecovery(
            JdbcTemplate jdbc,
            RefundService refunds,
            PaymentGateway gateway,
            PaymentService payments) {
        this.jdbc = jdbc;
        this.refunds = refunds;
        this.gateway = gateway;
        this.payments = payments;
    }

    @Scheduled(fixedDelayString = "${eduze.payment.refund-retry-ms:60000}")
    public void refunds() {
        var ids =
                jdbc.queryForList(
                        "SELECT id FROM commerce_refund WHERE status NOT IN ('COMPLETED','FAILED','PROVIDER_ABNORMAL') AND attempts<20 AND next_attempt_at<=? ORDER BY next_attempt_at LIMIT 20",
                        String.class,
                        Timestamp.from(Instant.now()));
        for (String id : ids) {
            try {
                refunds.advance(id);
            } catch (PlatformException error) {
                refunds.failedAttempt(id, error.getStatus());
            }
        }
    }

    @Scheduled(cron = "${eduze.payment.reconciliation-cron:0 15 3 * * *}", zone = "Asia/Shanghai")
    public void reconcile() {
        String cursor = "";
        while (true) {
            var ids =
                    jdbc.queryForList(
                            "SELECT id FROM commerce_order WHERE payment_status IN ('UNPAID','PAID','REFUNDING') AND id>? ORDER BY id LIMIT 200",
                            String.class,
                            cursor);
            if (ids.isEmpty()) {
                return;
            }
            for (String id : ids) {
                reconcileOne(id);
            }
            cursor = ids.get(ids.size() - 1);
        }
    }

    private void reconcileOne(String id) {

        try {
            var provider = gateway.queryOrder(id);
            String state = provider.path("trade_state").asText(provider.path("status").asText());
            if ("SUCCESS".equals(state)) {
                payments.paid(provider);
            }
            jdbc.update(
                    "INSERT INTO commerce_reconciliation(id,order_id,provider_status,result) VALUES(?,?,?,?)",
                    OrderService.id(),
                    id,
                    state,
                    "SUCCESS".equals(state) ? "MATCHED" : "REVIEW");
            jdbc.update("UPDATE commerce_order SET updated_at=CURRENT_TIMESTAMP WHERE id=?", id);
        } catch (PlatformException error) {
            jdbc.update(
                    "INSERT INTO commerce_reconciliation(id,order_id,provider_status,result) VALUES(?,?,?,'RETRY')",
                    OrderService.id(),
                    id,
                    "UNAVAILABLE");
        }
    }
}
