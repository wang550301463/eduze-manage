package com.eduze.platform.notification;

import com.eduze.platform.runtime.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    public record Message(
            String id,
            String title,
            String body,
            String businessType,
            String businessId,
            String path,
            String status,
            Instant readAt,
            Instant confirmedAt,
            Instant createdAt) {}

    public record Subscription(String templateKey, boolean accepted) {}

    private final JdbcTemplate jdbc;
    private final InternalClient client;

    public NotificationService(JdbcTemplate jdbc, InternalClient client) {
        this.jdbc = jdbc;
        this.client = client;
    }

    private boolean liveAccess(String studentId) {
        try {
            var result =
                    client.get(
                            "academic",
                            "/internal/academic/students/" + studentId + "/access",
                            com.fasterxml.jackson.databind.JsonNode.class);
            return result != null && result.path("allowed").asBoolean(false);
        } catch (PlatformException exception) {
            if (exception.getStatus() == 403 || exception.getStatus() == 404) {
                return false;
            }
            throw exception;
        }
    }

    private static Instant instant(java.sql.ResultSet rs, String name)
            throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp(name);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private org.springframework.jdbc.core.RowMapper<Message> mapper() {
        return (rs, n) ->
                new Message(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("body"),
                        rs.getString("business_type"),
                        rs.getString("business_id"),
                        rs.getString("path"),
                        rs.getString("status"),
                        instant(rs, "read_at"),
                        instant(rs, "confirmed_at"),
                        instant(rs, "created_at"));
    }

    public List<Message> list() {
        Actor actor = Actors.current();
        var messages =
                jdbc.query(
                        "SELECT id,title,body,business_type,business_id,path,status,read_at,confirmed_at,created_at FROM notification_message WHERE tenant_id=? AND recipient_id=? AND visible=1 ORDER BY created_at DESC LIMIT 100",
                        mapper(),
                        actor.tenantId(),
                        actor.userId());
        if (messages.isEmpty()) {
            return messages;
        }
        var children =
                jdbc.queryForList(
                        "SELECT id,student_id FROM notification_message WHERE tenant_id=? AND recipient_id=? AND visible=1 ORDER BY created_at DESC LIMIT 100",
                        actor.tenantId(),
                        actor.userId());
        Set<String> ids = new HashSet<>();
        Map<String, String> mapping = new HashMap<>();
        children.forEach(
                row -> {
                    String student = row.get("student_id").toString();
                    ids.add(student);
                    mapping.put(row.get("id").toString(), student);
                });
        var grants =
                client.post(
                        "academic",
                        "/internal/academic/students/batch-access",
                        Map.of("ids", ids),
                        com.fasterxml.jackson.databind.JsonNode.class);
        if (grants == null) {
            throw new PlatformException(503, "消息访问权暂时无法验证");
        }
        Set<String> allowed = new HashSet<>();
        grants.forEach(
                grant -> {
                    if (grant.path("allowed").asBoolean(false)) {
                        allowed.add(grant.path("id").asText());
                    }
                });
        return messages.stream()
                .filter(message -> allowed.contains(mapping.get(message.id())))
                .toList();
    }

    @Transactional
    public Message mark(String id, boolean confirm) {
        Actor actor = Actors.current();
        var students =
                jdbc.queryForList(
                        "SELECT student_id FROM notification_message WHERE id=? AND tenant_id=? AND recipient_id=? AND visible=1",
                        String.class,
                        id,
                        actor.tenantId(),
                        actor.userId());
        if (students.isEmpty() || !liveAccess(students.get(0))) {
            throw new PlatformException(404, "消息不存在");
        }
        String column = confirm ? "confirmed_at" : "read_at";
        int changed =
                jdbc.update(
                        "UPDATE notification_message SET "
                                + column
                                + "=COALESCE("
                                + column
                                + ",CURRENT_TIMESTAMP) WHERE id=? AND tenant_id=? AND recipient_id=? AND visible=1",
                        id,
                        actor.tenantId(),
                        actor.userId());
        if (changed == 0) {
            throw new PlatformException(404, "消息不存在");
        }
        return jdbc.query(
                        "SELECT id,title,body,business_type,business_id,path,status,read_at,confirmed_at,created_at FROM notification_message WHERE id=? AND tenant_id=? AND recipient_id=?",
                        mapper(),
                        id,
                        actor.tenantId(),
                        actor.userId())
                .get(0);
    }

    public List<Subscription> subscriptions() {
        Actor actor = Actors.current();
        return jdbc.query(
                "SELECT template_key,accepted FROM notification_subscription WHERE tenant_id=? AND user_id=?",
                (rs, n) -> new Subscription(rs.getString(1), rs.getBoolean(2)),
                actor.tenantId(),
                actor.userId());
    }

    @Transactional
    public Subscription subscribe(Subscription request) {
        Actor actor = Actors.current();
        if (!Set.of("LESSON_REMINDER", "LESSON_CHANGED", "LEAVE_RESULT", "PORTFOLIO_PUBLISHED")
                .contains(request.templateKey())) {
            throw new PlatformException(400, "订阅类型无效");
        }
        jdbc.update(
                "INSERT INTO notification_subscription(tenant_id,user_id,template_key,accepted) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE accepted=VALUES(accepted)",
                actor.tenantId(),
                actor.userId(),
                request.templateKey(),
                request.accepted());
        return request;
    }

    public List<Map<String, Object>> jobs() {
        Actor actor = Actors.current();
        actor.requirePermission("stat:read");
        return jdbc
                .queryForList(
                        "SELECT id,branch_id,recipient_id,business_type,business_id,status,attempts,last_error,next_at FROM notification_message WHERE tenant_id=? AND status IN ('FAILED','PENDING') ORDER BY created_at DESC LIMIT 100",
                        actor.tenantId())
                .stream()
                .filter(
                        row ->
                                actor.isSuperAdmin()
                                        || actor.branchIds()
                                                .contains(String.valueOf(row.get("branch_id"))))
                .toList();
    }

    public void retry(String id) {
        Actor actor = Actors.current();
        actor.requirePermission("stat:read");
        var rows =
                jdbc.queryForList(
                        "SELECT branch_id,status,last_error FROM notification_message WHERE id=? AND tenant_id=?",
                        id,
                        actor.tenantId());
        if (rows.isEmpty()) {
            throw new PlatformException(404, "任务不存在");
        }
        actor.requireBranch(String.valueOf(rows.get(0).get("branch_id")));
        if ("DELIVERY_UNKNOWN".equals(rows.get(0).get("last_error"))) {
            throw new PlatformException(409, "发送结果未知，需先人工核实，不能直接重复发送");
        }
        jdbc.update(
                "UPDATE notification_message SET status='PENDING',next_at=CURRENT_TIMESTAMP,attempts=0 WHERE id=? AND tenant_id=? AND status='FAILED'",
                id,
                actor.tenantId());
    }
}
