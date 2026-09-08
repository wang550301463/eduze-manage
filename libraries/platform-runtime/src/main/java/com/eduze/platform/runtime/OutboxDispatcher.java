package com.eduze.platform.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "eduze.runtime.outbox.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxDispatcher.class);
    private static final int MAX_ATTEMPTS = 8;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final InternalClient client;

    public OutboxDispatcher(JdbcTemplate jdbc, ObjectMapper json, InternalClient client) {
        this.jdbc = jdbc;
        this.json = json;
        this.client = client;
    }

    @Scheduled(fixedDelayString = "${eduze.runtime.outbox.delay-ms:5000}")
    public void dispatch() {
        List<String> candidates =
                jdbc.queryForList(
                        "SELECT event_id FROM platform_outbox WHERE (status='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP) OR (status='SENDING' AND lease_until<CURRENT_TIMESTAMP) ORDER BY created_at LIMIT 20",
                        String.class);
        for (String id : candidates) {
            Instant now = Instant.now();
            int claimed =
                    jdbc.update(
                            "UPDATE platform_outbox SET status='SENDING',lease_until=?,attempts=attempts+1 WHERE event_id=? AND ((status='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP) OR (status='SENDING' AND lease_until<CURRENT_TIMESTAMP))",
                            Timestamp.from(now.plusSeconds(60)),
                            id);
            if (claimed != 1) {
                continue;
            }
            var row =
                    jdbc.queryForMap(
                            "SELECT target,envelope,attempts FROM platform_outbox WHERE event_id=?",
                            id);
            int attempts = ((Number) row.get("attempts")).intValue();
            try {
                EventEnvelope event =
                        json.readValue(row.get("envelope").toString(), EventEnvelope.class);
                client.post(row.get("target").toString(), "/internal/events", event, Object.class);
                jdbc.update(
                        "UPDATE platform_outbox SET status='SENT',lease_until=NULL WHERE event_id=?",
                        id);
            } catch (Exception error) {
                long delay = Math.min(21600L, 30L * (1L << Math.min(attempts, 10)));
                jdbc.update(
                        "UPDATE platform_outbox SET status=?,next_attempt_at=?,lease_until=NULL WHERE event_id=?",
                        attempts >= MAX_ATTEMPTS ? "DEAD" : "PENDING",
                        Timestamp.from(now.plusSeconds(delay)),
                        id);
                LOGGER.warn(
                        "Event delivery failed eventId={} attempt={} errorType={}",
                        id,
                        attempts,
                        error.getClass().getSimpleName());
            }
        }
    }
}
