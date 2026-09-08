package com.eduze.platform.runtime;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Service-local backlog gauges; database failures are reported as NaN, never a healthy zero. */
@Component
public class EventMetrics implements MeterBinder {
    private final JdbcTemplate jdbc;

    public EventMetrics(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void bindTo(MeterRegistry registry) {
        for (String status : List.of("PENDING", "SENDING", "DEAD")) {
            Gauge.builder("eduze.outbox.events", () -> count(status))
                    .tag("status", status)
                    .register(registry);
        }
        Gauge.builder("eduze.outbox.oldest.seconds", this::oldest).register(registry);
    }

    private double count(String status) {
        try {
            Long count =
                    jdbc.queryForObject(
                            "SELECT COUNT(*) FROM platform_outbox WHERE status=?",
                            Long.class,
                            status);
            return count == null ? Double.NaN : count.doubleValue();
        } catch (DataAccessException error) {
            return Double.NaN;
        }
    }

    private double oldest() {
        try {
            Timestamp oldest =
                    jdbc.queryForObject(
                            "SELECT MIN(created_at) FROM platform_outbox WHERE status IN ('PENDING','SENDING','DEAD')",
                            Timestamp.class);
            return oldest == null
                    ? 0
                    : Math.max(0, Duration.between(oldest.toInstant(), Instant.now()).getSeconds());
        } catch (DataAccessException error) {
            return Double.NaN;
        }
    }
}
