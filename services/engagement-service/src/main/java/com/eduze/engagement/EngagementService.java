package com.eduze.engagement;

import static com.eduze.engagement.EngagementModels.*;

import com.eduze.platform.runtime.Actor;
import com.eduze.platform.runtime.InternalClient;
import com.eduze.platform.runtime.PlatformException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Studio discovery, lead progression and capacity-controlled activity registration. */
@Service
public class EngagementService {
    private final JdbcTemplate jdbc;
    private final InternalClient client;

    public EngagementService(JdbcTemplate jdbc, InternalClient client) {
        this.jdbc = jdbc;
        this.client = client;
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private static String text(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new PlatformException(400, "输入长度不正确");
        }
        return value.trim();
    }

    private static String optional(String value, int max) {
        return value == null || value.isBlank() ? "" : text(value, max);
    }

    private static void staff(Actor actor, String branch) {
        actor.requireBranch(branch);
        actor.requirePermission("engagement:write");
    }

    @Transactional
    public Studio saveStudio(Actor actor, StudioInput input) {
        staff(actor, input.branchId());
        var existing =
                jdbc.queryForList(
                        "SELECT tenant_id FROM studio_profile WHERE id=? FOR UPDATE",
                        input.branchId());
        if (!existing.isEmpty() && !actor.tenantId().equals(existing.get(0).get("tenant_id"))) {
            throw new PlatformException(403, "校区不属于当前机构");
        }
        String name = text(input.name(), 160),
                description = text(input.description(), 20000),
                address = text(input.address(), 500),
                phone = text(input.phone(), 32);
        if (existing.isEmpty()) {
            jdbc.update(
                    "INSERT INTO studio_profile(id,tenant_id,name,description,address,phone,published) VALUES(?,?,?,?,?,?,?)",
                    input.branchId(),
                    actor.tenantId(),
                    name,
                    description,
                    address,
                    phone,
                    input.published());
        } else {
            jdbc.update(
                    "UPDATE studio_profile SET name=?,description=?,address=?,phone=?,published=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?",
                    name,
                    description,
                    address,
                    phone,
                    input.published(),
                    input.branchId(),
                    actor.tenantId());
        }
        return new Studio(input.branchId(), name, description, address, phone);
    }

    public List<Studio> studios() {
        return jdbc.query(
                "SELECT id,name,description,address,phone FROM studio_profile WHERE published=true ORDER BY name LIMIT 100",
                (rs, n) ->
                        new Studio(
                                rs.getString(1),
                                rs.getString(2),
                                rs.getString(3),
                                rs.getString(4),
                                rs.getString(5)));
    }

    public Studio studio(String branchId) {
        return studios().stream()
                .filter(s -> s.branchId().equals(branchId))
                .findFirst()
                .orElseThrow(() -> new PlatformException(404, "画室未公开"));
    }

    @Transactional
    public Enquiry enquire(String branchId, EnquiryInput input) {
        String name = text(input.studentName(), 80), phone = text(input.phone(), 32);
        if (!phone.matches("[+0-9 -]{7,25}")
                || input.age() < 2
                || input.age() > 100
                || (input.website() != null && !input.website().isBlank())) {
            throw new PlatformException(400, "请检查咨询信息");
        }
        var owner =
                jdbc.queryForList(
                        "SELECT tenant_id FROM studio_profile WHERE id=? AND published=true FOR UPDATE",
                        branchId);
        if (owner.isEmpty()) {
            throw new PlatformException(404, "画室未公开");
        }
        if (jdbc.queryForObject(
                        "SELECT COUNT(*) FROM enquiry WHERE branch_id=? AND phone=? AND created_at>?",
                        Integer.class,
                        branchId,
                        phone,
                        Timestamp.from(Instant.now().minus(Duration.ofMinutes(30))))
                > 0) {
            throw new PlatformException(429, "已收到咨询，请稍后再试");
        }
        String enquiryId = id();
        jdbc.update(
                "INSERT INTO enquiry(id,tenant_id,branch_id,student_name,phone,age,source,source_id,referrer_id,status) VALUES(?,?,?,?,?,?,?,?,?,'NEW')",
                enquiryId,
                owner.get(0).get("tenant_id"),
                branchId,
                name,
                phone,
                input.age(),
                optional(input.source(), 120),
                optional(input.sourceId(), 120),
                optional(input.referrerId(), 64));
        return new Enquiry(
                enquiryId,
                branchId,
                name,
                phone,
                input.age(),
                input.source(),
                input.sourceId(),
                "NEW",
                null,
                null);
    }

    public List<Enquiry> enquiries(Actor actor, String branchId) {
        staff(actor, branchId);
        return jdbc.query(
                "SELECT id,branch_id,student_name,phone,age,source,source_id,status,assignee_id,reservation_id FROM enquiry WHERE tenant_id=? AND branch_id=? ORDER BY updated_at DESC LIMIT 200",
                (rs, n) ->
                        new Enquiry(
                                rs.getString(1),
                                rs.getString(2),
                                rs.getString(3),
                                rs.getString(4),
                                rs.getInt(5),
                                rs.getString(6),
                                rs.getString(7),
                                rs.getString(8),
                                rs.getString(9),
                                rs.getString(10)),
                actor.tenantId(),
                branchId);
    }

    private Map<String, Object> enquiry(Actor actor, String enquiryId) {
        var rows =
                jdbc.queryForList(
                        "SELECT id,branch_id,student_name,phone,status,referrer_id FROM enquiry WHERE id=? AND tenant_id=? FOR UPDATE",
                        enquiryId,
                        actor.tenantId());
        if (rows.isEmpty()) {
            throw new PlatformException(404, "咨询不存在");
        }
        staff(actor, (String) rows.get(0).get("branch_id"));
        return rows.get(0);
    }

    @Transactional
    public void followup(Actor actor, String enquiryId, FollowupInput input) {
        var lead = enquiry(actor, enquiryId);
        boolean enrolled = "ENROLLED".equals(lead.get("status"));
        String effectiveStatus = enrolled ? "ENROLLED" : input.status();
        if (!(enrolled && "ENROLLED".equals(input.status()))
                && !Set.of("NEW", "CONTACTED", "TRIAL_PENDING", "TRIAL_BOOKED", "ATTENDED", "LOST")
                        .contains(input.status())) {
            throw new PlatformException(400, "状态不正确，报名状态由教务确认");
        }
        jdbc.update(
                "INSERT INTO enquiry_followup(id,enquiry_id,author_id,note,status,next_at) VALUES(?,?,?,?,?,?)",
                id(),
                enquiryId,
                actor.userId(),
                text(input.note(), 4000),
                effectiveStatus,
                input.nextAt() == null ? null : Timestamp.from(input.nextAt()));
        jdbc.update(
                "UPDATE enquiry SET status=?,assignee_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                effectiveStatus,
                optional(input.assigneeId(), 64),
                enquiryId);
    }

    public List<Map<String, Object>> history(Actor actor, String enquiryId) {
        enquiry(actor, enquiryId);
        return jdbc.queryForList(
                "SELECT author_id AS authorId,note,status,next_at AS nextAt,created_at AS createdAt FROM enquiry_followup WHERE enquiry_id=? ORDER BY created_at DESC LIMIT 200",
                enquiryId);
    }

    @Transactional
    public Map<String, Object> reserveTrial(Actor actor, String enquiryId, TrialInput input) {
        var lead = enquiry(actor, enquiryId);
        if ("ENROLLED".equals(lead.get("status"))) {
            throw new PlatformException(409, "该咨询已报名，不能重新安排试听");
        }
        String session = text(input.sessionId(), 64);
        String reservation = "trial-" + enquiryId;
        Map<?, ?> result =
                client.post(
                        "academic",
                        "/internal/academic/trial-reservations",
                        Map.of(
                                "reservationId",
                                reservation,
                                "tenantId",
                                actor.tenantId(),
                                "branchId",
                                lead.get("branch_id"),
                                "sessionId",
                                session,
                                "studentName",
                                lead.get("student_name"),
                                "contactPhone",
                                lead.get("phone")),
                        Map.class);
        if (result == null || !"CONFIRMED".equals(result.get("status"))) {
            throw new PlatformException(409, "试听名额尚未确认");
        }
        jdbc.update(
                "UPDATE enquiry SET status='TRIAL_BOOKED',reservation_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                reservation,
                enquiryId);
        return Map.of("reservationId", reservation, "status", "CONFIRMED");
    }

    @Transactional
    public Activity saveActivity(Actor actor, ActivityInput input) {
        staff(actor, input.branchId());
        if (input.capacity() < 1 || input.capacity() > 10000) {
            throw new PlatformException(400, "名额应为1至10000");
        }
        Instant starts;
        try {
            starts = Instant.parse(input.startsAt());
        } catch (RuntimeException error) {
            throw new PlatformException(400, "活动时间需包含时区");
        }
        if (!starts.isAfter(Instant.now())) {
            throw new PlatformException(400, "活动时间已过");
        }
        String activity = id();
        jdbc.update(
                "INSERT INTO engagement_activity(id,tenant_id,branch_id,title,description,starts_at,capacity,published) VALUES(?,?,?,?,?,?,?,?)",
                activity,
                actor.tenantId(),
                input.branchId(),
                text(input.title(), 160),
                text(input.description(), 20000),
                Timestamp.from(starts),
                input.capacity(),
                input.published());
        return activity(activity);
    }

    public Activity activity(String activityId) {
        var list =
                jdbc.query(
                        "SELECT id,branch_id,title,description,starts_at,capacity,reserved,published FROM engagement_activity WHERE id=?",
                        (rs, n) ->
                                new Activity(
                                        rs.getString(1),
                                        rs.getString(2),
                                        rs.getString(3),
                                        rs.getString(4),
                                        rs.getTimestamp(5).toInstant(),
                                        rs.getInt(6),
                                        rs.getInt(7),
                                        rs.getBoolean(8)),
                        activityId);
        if (list.isEmpty()) {
            throw new PlatformException(404, "活动不存在");
        }
        return list.get(0);
    }

    public List<Activity> activities(String branchId) {
        return jdbc.query(
                "SELECT id,branch_id,title,description,starts_at,capacity,reserved,published FROM engagement_activity WHERE branch_id=? AND published=true ORDER BY starts_at DESC LIMIT 100",
                (rs, n) ->
                        new Activity(
                                rs.getString(1),
                                rs.getString(2),
                                rs.getString(3),
                                rs.getString(4),
                                rs.getTimestamp(5).toInstant(),
                                rs.getInt(6),
                                rs.getInt(7),
                                rs.getBoolean(8)),
                branchId);
    }

    @Transactional
    public Signup signup(Actor actor, String activityId) {
        var activityRows =
                jdbc.queryForList(
                        "SELECT tenant_id,published,starts_at FROM engagement_activity WHERE id=? FOR UPDATE",
                        activityId);
        if (activityRows.isEmpty()
                || !actor.tenantId().equals(activityRows.get(0).get("tenant_id"))) {
            throw new PlatformException(404, "活动不存在");
        }
        var existing =
                jdbc.query(
                        "SELECT id,activity_id,user_id,status FROM activity_signup WHERE activity_id=? AND user_id=?",
                        (rs, n) ->
                                new Signup(
                                        rs.getString(1),
                                        rs.getString(2),
                                        rs.getString(3),
                                        rs.getString(4)),
                        activityId,
                        actor.userId());
        if (!existing.isEmpty() && !"CANCELLED".equals(existing.get(0).status())) {
            return existing.get(0);
        }
        if (jdbc.update(
                        "UPDATE engagement_activity SET reserved=reserved+1 WHERE id=? AND published=true AND reserved<capacity AND starts_at>?",
                        activityId,
                        Timestamp.from(Instant.now()))
                != 1) {
            throw new PlatformException(409, "活动名额不足或已结束");
        }
        String signupId = existing.isEmpty() ? id() : existing.get(0).id();
        if (existing.isEmpty()) {
            jdbc.update(
                    "INSERT INTO activity_signup(id,activity_id,tenant_id,user_id,status) VALUES(?,?,?,?,'CONFIRMED')",
                    signupId,
                    activityId,
                    actor.tenantId(),
                    actor.userId());
        } else {
            jdbc.update("UPDATE activity_signup SET status='CONFIRMED' WHERE id=?", signupId);
        }
        return new Signup(signupId, activityId, actor.userId(), "CONFIRMED");
    }

    @Transactional
    public void cancelSignup(Actor actor, String signupId) {
        var rows =
                jdbc.queryForList(
                        "SELECT activity_id,status FROM activity_signup WHERE id=? AND tenant_id=? AND user_id=? FOR UPDATE",
                        signupId,
                        actor.tenantId(),
                        actor.userId());
        if (rows.isEmpty()) {
            throw new PlatformException(404, "报名不存在");
        }
        if ("CONFIRMED".equals(rows.get(0).get("status"))) {
            jdbc.update("UPDATE activity_signup SET status='CANCELLED' WHERE id=?", signupId);
            jdbc.update(
                    "UPDATE engagement_activity SET reserved=reserved-1 WHERE id=? AND reserved>0",
                    rows.get(0).get("activity_id"));
        }
    }

    @Transactional
    public String renewal(Actor actor, RenewalInput input) {
        staff(actor, input.branchId());
        if (!Set.of("OPEN", "CONTACTED", "CLOSED").contains(input.status())) {
            throw new PlatformException(400, "续费状态无效");
        }
        Map<?, ?> access =
                client.get(
                        "academic",
                        "/internal/academic/students/" + text(input.studentId(), 64) + "/access",
                        Map.class);
        if (access == null
                || !Boolean.TRUE.equals(access.get("allowed"))
                || !input.branchId().equals(access.get("branchId"))) {
            throw new PlatformException(403, "无法访问学生");
        }
        String renewal = id();
        jdbc.update(
                "INSERT INTO renewal_followup(id,tenant_id,branch_id,student_id,assignee_id,note,next_at,status) VALUES(?,?,?,?,?,?,?,?)",
                renewal,
                actor.tenantId(),
                input.branchId(),
                input.studentId(),
                text(input.assigneeId(), 64),
                text(input.note(), 4000),
                input.nextAt() == null ? null : Timestamp.from(input.nextAt()),
                input.status());
        return renewal;
    }

    public List<Map<String, Object>> renewals(Actor actor, String branch) {
        staff(actor, branch);
        return jdbc.queryForList(
                "SELECT id,student_id AS studentId,assignee_id AS assigneeId,note,next_at AS nextAt,status FROM renewal_followup WHERE tenant_id=? AND branch_id=? ORDER BY updated_at DESC LIMIT 200",
                actor.tenantId(),
                branch);
    }

    @Transactional
    public Map<String, Object> enroll(Actor actor, String enquiryId, String classGroupId) {
        enquiry(actor, enquiryId);
        String group = text(classGroupId, 64);
        var rows =
                jdbc.queryForList(
                        "SELECT reservation_id FROM enquiry WHERE id=? AND tenant_id=?",
                        enquiryId,
                        actor.tenantId());
        Object reservation = rows.get(0).get("reservation_id");
        if (reservation == null) {
            throw new PlatformException(409, "请先确认试听安排");
        }
        Map<?, ?> result =
                client.post(
                        "academic",
                        "/internal/academic/trial-reservations/" + reservation + "/enroll",
                        Map.of(
                                "enquiryId",
                                enquiryId,
                                "enrollmentId",
                                "enroll-" + enquiryId,
                                "classGroupId",
                                group),
                        Map.class);
        if (result == null || !"ENROLLED".equals(result.get("status"))) {
            throw new PlatformException(503, "报名结果待核对");
        }
        return Map.of(
                "status",
                "ENROLLED",
                "studentId",
                result.get("studentId"),
                "enrollmentId",
                "enroll-" + enquiryId);
    }
}
