package com.eduze.engagement;

import com.eduze.platform.runtime.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Rewards become eligible only after an authoritative enrollment event. */
@Component
public class EnrollmentHandler implements EventHandler {
    private final JdbcTemplate jdbc;

    public EnrollmentHandler(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean supports(String type) {
        return "academic.enrollment-confirmed".equals(type);
    }

    @Override
    public void handle(EventEnvelope event) {
        var payload = event.payload();
        String enquiry = payload.path("enquiryId").asText();
        String enrollment = payload.path("enrollmentId").asText();
        if (enquiry.isBlank() || enrollment.isBlank()) {
            throw new PlatformException(400, "报名事件缺少关联标识");
        }
        var rows =
                jdbc.queryForList(
                        "SELECT referrer_id FROM enquiry WHERE id=? AND tenant_id=? AND branch_id=? FOR UPDATE",
                        enquiry,
                        event.tenantId(),
                        event.branchId());
        if (rows.isEmpty()) {
            throw new PlatformException(404, "咨询不存在");
        }
        jdbc.update(
                "UPDATE enquiry SET status='ENROLLED',updated_at=CURRENT_TIMESTAMP WHERE id=?",
                enquiry);
        String referrer = (String) rows.get(0).get("referrer_id");
        if (!referrer.isBlank()
                && jdbc.queryForObject(
                                "SELECT COUNT(*) FROM referral_reward WHERE enquiry_id=?",
                                Integer.class,
                                enquiry)
                        == 0) {
            jdbc.update(
                    "INSERT INTO referral_reward(enquiry_id,tenant_id,referrer_id,enrollment_id,status) VALUES(?,?,?,?,'ELIGIBLE')",
                    enquiry,
                    event.tenantId(),
                    referrer,
                    enrollment);
        }
    }
}
