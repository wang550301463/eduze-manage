package com.eduze.platform.notification;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEvents implements EventHandler {
    private final JdbcTemplate jdbc;
    private final InternalClient client;

    public NotificationEvents(JdbcTemplate jdbc, InternalClient client) {
        this.jdbc = jdbc;
        this.client = client;
    }

    public boolean supports(String type) {
        return Set.of(
                        "portfolio.published",
                        "portfolio.withdrawn",
                        "portfolio.classroom-published",
                        "portfolio.classroom-withdrawn",
                        "academic.family-authorization-changed",
                        "academic.lesson-changed",
                        "academic.leave-approved")
                .contains(type);
    }

    public void handle(EventEnvelope event) {
        JsonNode payload = event.payload();
        if (event.type().equals("academic.family-authorization-changed")) {
            if (!Set.of("APPROVED", "ACTIVE").contains(payload.path("status").asText())) {
                JsonNode current =
                        client.get(
                                "academic",
                                "/internal/academic/students/"
                                        + payload.path("studentId").asText()
                                        + "/recipients",
                                JsonNode.class);
                if (current == null
                        || !event.tenantId().equals(current.path("tenantId").asText())) {
                    throw new PlatformException(503, "当前家庭授权暂时无法核实");
                }
                for (JsonNode recipient : current.path("recipients")) {
                    if (payload.path("userId").asText().equals(recipient.path("userId").asText())) {
                        return; // A newer accepted invitation supersedes an older revocation event.
                    }
                }
                jdbc.update(
                        "UPDATE notification_message SET visible=0,status=CASE WHEN status IN ('PENDING','SENDING') THEN 'SKIPPED' ELSE status END WHERE tenant_id=? AND student_id=? AND recipient_id=?",
                        event.tenantId(),
                        payload.path("studentId").asText(),
                        payload.path("userId").asText());
            }
            return;
        }
        String aggregateKey =
                (event.type().startsWith("portfolio.classroom-")
                                ? "classroom"
                                : event.type().startsWith("portfolio.")
                                        ? "portfolio"
                                        : event.type().equals("academic.lesson-changed")
                                                ? "lesson"
                                                : "leave")
                        + ":"
                        + event.aggregateId();
        long revision = payload.path("revision").asLong(payload.path("version").asLong(0));
        try {
            jdbc.update(
                    "INSERT INTO notification_cursor(tenant_id,aggregate_key,revision) VALUES(?,?,-1)",
                    event.tenantId(),
                    aggregateKey);
        } catch (DuplicateKeyException duplicate) {
            /* Lock the existing cursor below. */
        }
        Long current =
                jdbc.queryForObject(
                        "SELECT revision FROM notification_cursor WHERE tenant_id=? AND aggregate_key=? FOR UPDATE",
                        Long.class,
                        event.tenantId(),
                        aggregateKey);
        if (current != null && !NotificationPolicy.acceptRevision(current, revision)) {
            return;
        }
        jdbc.update(
                "UPDATE notification_cursor SET revision=? WHERE tenant_id=? AND aggregate_key=?",
                revision,
                event.tenantId(),
                aggregateKey);
        jdbc.update(
                "UPDATE notification_message SET visible=0,status=CASE WHEN status IN ('PENDING','SENDING') THEN 'SKIPPED' ELSE status END WHERE tenant_id=? AND aggregate_key=?",
                event.tenantId(),
                aggregateKey);
        if (event.type().endsWith("withdrawn")) {
            return;
        }
        if (event.type().equals("academic.lesson-changed")) {
            String status = payload.path("status").asText();
            boolean cancelled = Set.of("CANCELLED", "CANCELED").contains(status);
            Instant start =
                    payload.hasNonNull("startTime")
                            ? parse(payload.get("startTime").asText())
                            : Instant.now();
            for (JsonNode student : payload.path("studentIds")) {
                String id = student.asText();
                if (revision > 0 || cancelled) {
                    create(
                            event,
                            aggregateKey,
                            id,
                            "LESSON_CHANGED",
                            cancelled ? "课程已取消" : "课程安排有更新",
                            "请查看最新课表，确认上课安排。",
                            "pages/family/index?studentId=" + id,
                            Instant.now(),
                            null);
                }
                if (!cancelled && NotificationPolicy.canSendReminder(start, Instant.now())) {
                    create(
                            event,
                            aggregateKey,
                            id,
                            "LESSON_REMINDER",
                            "上课提醒",
                            "上课时间：" + start.atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime(),
                            "pages/family/index?studentId=" + id,
                            start.minusSeconds(86400).isBefore(Instant.now())
                                    ? Instant.now()
                                    : start.minusSeconds(86400),
                            start);
                }
            }
            return;
        }
        String studentId = payload.path("studentId").asText();
        boolean portfolio = event.type().startsWith("portfolio.");
        create(
                event,
                aggregateKey,
                studentId,
                portfolio ? "PORTFOLIO_PUBLISHED" : "LEAVE_RESULT",
                portfolio ? "新的主题课效已发布" : "请假处理结果",
                portfolio ? "老师已整理孩子的创作过程与点评，点击查看。" : "请查看本次请假申请的处理结果。",
                portfolio ? "pages/family/index?studentId=" + studentId : "pages/family/index",
                Instant.now(),
                null);
    }

    private Instant parse(String value) {
        try {
            return java.time.OffsetDateTime.parse(value).toInstant();
        } catch (java.time.format.DateTimeParseException ignored) {
            return LocalDateTime.parse(value).atZone(ZoneId.of("Asia/Shanghai")).toInstant();
        }
    }

    private void create(
            EventEnvelope event,
            String aggregate,
            String student,
            String type,
            String title,
            String body,
            String path,
            Instant next,
            Instant expires) {
        JsonNode families =
                client.get(
                        "academic",
                        "/internal/academic/students/" + student + "/recipients",
                        JsonNode.class);
        if (families == null || !event.tenantId().equals(families.path("tenantId").asText())) {
            throw new PlatformException(403, "消息机构不匹配");
        }
        for (JsonNode recipient : families.path("recipients")) {
            String user = recipient.path("userId").asText();
            if (user.isBlank()) {
                continue;
            }
            jdbc.update(
                    "INSERT INTO notification_message(id,tenant_id,branch_id,recipient_id,student_id,title,body,business_type,business_id,aggregate_key,path,status,attempts,next_at,expires_at,visible,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,'PENDING',0,?,?,1,CURRENT_TIMESTAMP)",
                    UUID.randomUUID().toString(),
                    event.tenantId(),
                    families.path("branchId").asText(),
                    user,
                    student,
                    title,
                    body,
                    type,
                    event.aggregateId(),
                    aggregate,
                    path,
                    Timestamp.from(next),
                    expires == null ? null : Timestamp.from(expires));
        }
    }
}
