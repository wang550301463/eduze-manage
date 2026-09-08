package com.eduze.engagement;

import static com.eduze.engagement.EngagementModels.*;

import com.eduze.platform.runtime.*;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EngagementOperations {
    private final JdbcTemplate jdbc;

    public EngagementOperations(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private void staff(Actor actor, String branch) {
        actor.requireBranch(branch);
        actor.requirePermission("engagement:write");
    }

    public List<Map<String, Object>> signups(Actor actor) {
        return jdbc.queryForList(
                "SELECT s.id,s.activity_id AS activityId,a.title AS activityTitle,a.starts_at AS startsAt,s.status FROM activity_signup s JOIN engagement_activity a ON a.id=s.activity_id WHERE s.user_id=? AND s.tenant_id=? ORDER BY s.created_at DESC LIMIT 100",
                actor.userId(),
                actor.tenantId());
    }

    public List<Map<String, Object>> activitySignups(Actor actor, String id) {
        var activity =
                jdbc.queryForList(
                        "SELECT branch_id FROM engagement_activity WHERE id=? AND tenant_id=?",
                        id,
                        actor.tenantId());
        if (activity.isEmpty()) {
            throw new PlatformException(404, "活动不存在");
        }
        staff(actor, (String) activity.get(0).get("branch_id"));
        return jdbc.queryForList(
                "SELECT id,user_id AS userId,status,created_at AS createdAt FROM activity_signup WHERE activity_id=? AND tenant_id=? ORDER BY created_at LIMIT 1000",
                id,
                actor.tenantId());
    }

    @Transactional
    public void checkin(Actor actor, String id) {
        var rows =
                jdbc.queryForList(
                        "SELECT a.branch_id,s.status FROM activity_signup s JOIN engagement_activity a ON a.id=s.activity_id WHERE s.id=? AND s.tenant_id=? FOR UPDATE",
                        id,
                        actor.tenantId());
        if (rows.isEmpty()) {
            throw new PlatformException(404, "报名不存在");
        }
        staff(actor, (String) rows.get(0).get("branch_id"));
        if ("CHECKED_IN".equals(rows.get(0).get("status"))) {
            return;
        }
        if (jdbc.update(
                        "UPDATE activity_signup SET status='CHECKED_IN' WHERE id=? AND status='CONFIRMED'",
                        id)
                != 1) {
            throw new PlatformException(409, "报名已取消");
        }
    }

    @Transactional
    public void updateRenewal(Actor actor, String id, RenewalInput input) {
        staff(actor, input.branchId());
        if (input.note() == null
                || input.note().isBlank()
                || input.note().length() > 4000
                || input.assigneeId() == null
                || input.assigneeId().isBlank()
                || !Set.of("OPEN", "CONTACTED", "CLOSED").contains(input.status())) {
            throw new PlatformException(400, "续费跟进内容无效");
        }
        if (jdbc.update(
                        "UPDATE renewal_followup SET assignee_id=?,note=?,next_at=?,status=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=? AND branch_id=? AND student_id=?",
                        input.assigneeId(),
                        input.note(),
                        input.nextAt() == null ? null : Timestamp.from(input.nextAt()),
                        input.status(),
                        id,
                        actor.tenantId(),
                        input.branchId(),
                        input.studentId())
                != 1) {
            throw new PlatformException(404, "续费跟进不存在");
        }
    }

    public Map<String, Object> dashboard(Actor actor, String branch) {
        staff(actor, branch);
        var stages =
                jdbc.queryForList(
                        "SELECT status,COUNT(*) AS count FROM enquiry WHERE tenant_id=? AND branch_id=? GROUP BY status",
                        actor.tenantId(),
                        branch);
        var sources =
                jdbc.queryForList(
                        "SELECT source,COUNT(*) AS enquiries,SUM(CASE WHEN status='ENROLLED' THEN 1 ELSE 0 END) AS enrollments FROM enquiry WHERE tenant_id=? AND branch_id=? GROUP BY source",
                        actor.tenantId(),
                        branch);
        return Map.of(
                "stages",
                stages,
                "sources",
                sources,
                "definition",
                "咨询按创建记录计数，报名仅来自教务已确认事件；转化率=已报名咨询数/咨询总数");
    }

    public List<Map<String, Object>> referrals(Actor actor, String branch) {
        staff(actor, branch);
        return jdbc.queryForList(
                "SELECT r.enquiry_id AS enquiryId,r.referrer_id AS referrerId,r.enrollment_id AS enrollmentId,r.status,r.fulfillment_note AS fulfillmentNote FROM referral_reward r JOIN enquiry e ON e.id=r.enquiry_id WHERE r.tenant_id=? AND e.branch_id=? ORDER BY r.created_at DESC LIMIT 200",
                actor.tenantId(),
                branch);
    }

    @Transactional
    public void fulfillReferral(Actor actor, String enquiry, String note) {
        if (note == null || note.isBlank() || note.length() > 1000) {
            throw new PlatformException(400, "请填写奖励发放记录");
        }
        var rows =
                jdbc.queryForList(
                        "SELECT branch_id FROM enquiry WHERE id=? AND tenant_id=?",
                        enquiry,
                        actor.tenantId());
        if (rows.isEmpty()) {
            throw new PlatformException(404, "咨询不存在");
        }
        staff(actor, (String) rows.get(0).get("branch_id"));
        if (jdbc.update(
                        "UPDATE referral_reward SET status='FULFILLED',fulfillment_note=?,fulfilled_by=? WHERE enquiry_id=? AND tenant_id=? AND status='ELIGIBLE'",
                        note,
                        actor.userId(),
                        enquiry,
                        actor.tenantId())
                != 1) {
            throw new PlatformException(409, "奖励尚未满足报名条件或已发放");
        }
    }
}
