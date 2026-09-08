package com.eduze.platform.runtime;

import static org.junit.jupiter.api.Assertions.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class EventMetricsTest {
    @Test
    void backlogSeparatesDeadPendingAndUnavailableDatabase() {
        var jdbc =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                "jdbc:h2:mem:metrics;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE platform_outbox(status VARCHAR(20),created_at TIMESTAMP)");
        jdbc.update(
                "INSERT INTO platform_outbox VALUES('PENDING',CURRENT_TIMESTAMP),('DEAD',CURRENT_TIMESTAMP),('SENT',CURRENT_TIMESTAMP)");
        var registry = new SimpleMeterRegistry();
        try {
            new EventMetrics(jdbc).bindTo(registry);
            assertEquals(
                    1,
                    registry.get("eduze.outbox.events").tag("status", "PENDING").gauge().value());
            assertEquals(
                    1, registry.get("eduze.outbox.events").tag("status", "DEAD").gauge().value());
            jdbc.execute("DROP TABLE platform_outbox");
            assertTrue(
                    Double.isNaN(
                            registry.get("eduze.outbox.events")
                                    .tag("status", "PENDING")
                                    .gauge()
                                    .value()));
        } finally {
            registry.close();
        }
    }
}
