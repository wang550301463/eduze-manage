package com.eduze.platform.notification;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationDispatcher {
    private final JdbcTemplate jdbc;
    private final InternalClient client;
    private final WechatSender sender;

    public NotificationDispatcher(JdbcTemplate jdbc, InternalClient client, WechatSender sender) {
        this.jdbc = jdbc;
        this.client = client;
        this.sender = sender;
    }

    @Scheduled(fixedDelayString = "${eduze.notifications.delay-ms:10000}")
    public void dispatch() {
        // Crash after provider submission has an unknown outcome: do not blindly replay it.
        jdbc.update(
                "UPDATE notification_message SET status='FAILED',last_error='DELIVERY_UNKNOWN' WHERE status='SENDING' AND lease_until<CURRENT_TIMESTAMP");
        List<String> ids =
                jdbc.queryForList(
                        "SELECT id FROM notification_message WHERE status='PENDING' AND visible=1 AND next_at<=CURRENT_TIMESTAMP ORDER BY next_at LIMIT 20",
                        String.class);
        for (String id : ids) {
            if (jdbc.update(
                            "UPDATE notification_message SET status='SENDING',lease_until=?,attempts=attempts+1 WHERE id=? AND status='PENDING' AND visible=1",
                            Timestamp.from(Instant.now().plusSeconds(120)),
                            id)
                    != 1) {
                continue;
            }
            var row =
                    jdbc.queryForMap(
                            "SELECT tenant_id,recipient_id,student_id,title,body,path,business_type,business_id,aggregate_key,expires_at,attempts FROM notification_message WHERE id=?",
                            id);
            try {
                if (row.get("expires_at") instanceof Timestamp expires
                        && !expires.toInstant().isAfter(Instant.now())) {
                    finish(id, "SKIPPED", "EXPIRED");
                    continue;
                }
                String tenant = row.get("tenant_id").toString(),
                        user = row.get("recipient_id").toString(),
                        student = row.get("student_id").toString();
                JsonNode family =
                        client.get(
                                "academic",
                                "/internal/academic/students/" + student + "/recipients",
                                JsonNode.class);
                boolean allowed = false;
                if (family != null && tenant.equals(family.path("tenantId").asText())) {
                    for (JsonNode recipient : family.path("recipients")) {
                        if (user.equals(recipient.path("userId").asText())) {
                            allowed = true;
                        }
                    }
                }
                if (!allowed) {
                    finish(id, "SKIPPED", "AUTHORIZATION_REVOKED");
                    continue;
                }
                if ("LESSON_REMINDER".equals(row.get("business_type"))) {
                    JsonNode lesson =
                            client.get(
                                    "academic",
                                    "/internal/academic/lessons/"
                                            + row.get("business_id")
                                            + "/students/"
                                            + student
                                            + "/notification-access",
                                    JsonNode.class);
                    if (lesson == null) {
                        throw new PlatformException(503, "课次名单暂时无法核实");
                    }
                    Long queuedRevision =
                            jdbc.queryForObject(
                                    "SELECT revision FROM notification_cursor WHERE tenant_id=? AND aggregate_key=?",
                                    Long.class,
                                    tenant,
                                    row.get("aggregate_key"));
                    if (!lesson.path("allowed").asBoolean(false)
                            || queuedRevision == null
                            || queuedRevision.longValue() != lesson.path("revision").asLong(-1)) {
                        finish(id, "SKIPPED", "STALE_SCHEDULE");
                        continue;
                    }
                }
                Integer accepted =
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM notification_subscription WHERE tenant_id=? AND user_id=? AND template_key=? AND accepted=1",
                                Integer.class,
                                tenant,
                                user,
                                row.get("business_type"));
                if (accepted == null || accepted == 0) {
                    finish(id, "SKIPPED", "NOT_SUBSCRIBED");
                    continue;
                }
                JsonNode users =
                        client.post(
                                "identity",
                                "/internal/identity/wechat/recipients",
                                Map.of("userIds", List.of(user)),
                                JsonNode.class);
                String openId = null;
                if (users != null) {
                    for (JsonNode item : users) {
                        if (user.equals(item.path("userId").asText())) {
                            openId = item.path("openId").asText(null);
                        }
                    }
                }
                if (openId == null) {
                    finish(id, "SKIPPED", "NO_WECHAT_IDENTITY");
                    continue;
                }
                String result =
                        sender.send(
                                openId,
                                row.get("business_type").toString(),
                                row.get("path").toString(),
                                Map.of(
                                        "title",
                                        row.get("title").toString(),
                                        "body",
                                        row.get("body").toString()));
                if (result.equals("RETRY")) {
                    int attempts = ((Number) row.get("attempts")).intValue();
                    jdbc.update(
                            "UPDATE notification_message SET status=?,next_at=?,last_error='PROVIDER_REJECTED' WHERE id=? AND status='SENDING'",
                            attempts >= 5 ? "FAILED" : "PENDING",
                            Timestamp.from(Instant.now().plusSeconds(60L * (1L << attempts))),
                            id);
                } else {
                    finish(id, result, result.equals("SENT") ? null : "PROVIDER_NOT_SENT");
                }
            } catch (PlatformException error) {
                // Credential and domain lookups fail before send; unknown provider errors are never
                // logged with URLs.
                finish(id, "FAILED", "DEPENDENCY_UNAVAILABLE");
            } catch (Exception error) {
                finish(id, "FAILED", "DELIVERY_UNKNOWN");
            }
        }
    }

    private void finish(String id, String status, String reason) {
        jdbc.update(
                "UPDATE notification_message SET status=?,last_error=?,lease_until=NULL WHERE id=? AND status='SENDING'",
                status,
                reason,
                id);
    }
}
