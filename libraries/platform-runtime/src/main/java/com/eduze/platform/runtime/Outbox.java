package com.eduze.platform.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class Outbox {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public Outbox(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void enqueue(
            String target,
            String type,
            String tenantId,
            String branchId,
            String aggregateId,
            Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Outbox requires a business transaction");
        }
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        EventEnvelope event =
                new EventEnvelope(
                        id,
                        type,
                        1,
                        tenantId,
                        branchId,
                        aggregateId,
                        now,
                        MDC.get("traceId"),
                        json.valueToTree(payload));
        try {
            jdbc.update(
                    "INSERT INTO platform_outbox(event_id,target,envelope,status,attempts,next_attempt_at,created_at) VALUES(?,?,?,'PENDING',0,?,?)",
                    id,
                    target,
                    json.writeValueAsString(event),
                    Timestamp.from(now),
                    Timestamp.from(now));
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("Invalid event payload", error);
        }
    }
}
