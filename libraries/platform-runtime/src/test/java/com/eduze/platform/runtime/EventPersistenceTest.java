package com.eduze.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class EventPersistenceTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private ObjectMapper json;

    @BeforeEach
    void prepare() {
        var dataSource =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:events;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute(
                "CREATE TABLE platform_outbox(event_id varchar(36) primary key, target varchar(40), envelope clob, status varchar(20), attempts int, next_attempt_at timestamp, lease_until timestamp, created_at timestamp)");
        jdbc.execute(
                "CREATE TABLE platform_inbox(event_id varchar(36) primary key, event_type varchar(80), processed_at timestamp)");
        jdbc.execute("CREATE TABLE effects(id int primary key)");
        tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        json = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void businessRollbackAlsoRollsBackOutbox() {
        Outbox outbox = new Outbox(jdbc, json);
        assertThatThrownBy(
                        () ->
                                tx.execute(
                                        status -> {
                                            outbox.enqueue(
                                                    "notification",
                                                    "portfolio.published",
                                                    "1",
                                                    "2",
                                                    "record",
                                                    Map.of("recordId", "record"));
                                            throw new IllegalStateException("business rejected");
                                        }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_outbox", Integer.class))
                .isZero();
    }

    @Test
    void eventReplayDoesNotRepeatEffectAndFailureIsRetryable() {
        EventHandler handler =
                new EventHandler() {
                    public boolean supports(String type) {
                        return type.equals("effect");
                    }

                    public void handle(EventEnvelope event) {
                        jdbc.update(
                                "INSERT INTO effects VALUES (?)",
                                event.payload().get("id").asInt());
                    }
                };
        Inbox inbox = new Inbox(jdbc, tx, List.of(handler));
        EventEnvelope event =
                new EventEnvelope(
                        "same",
                        "effect",
                        1,
                        "1",
                        "2",
                        "record",
                        Instant.now(),
                        "trace",
                        json.valueToTree(Map.of("id", 1)));
        assertThat(inbox.receive(event)).isTrue();
        assertThat(inbox.receive(event)).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM effects", Integer.class)).isEqualTo(1);
        EventEnvelope failed =
                new EventEnvelope(
                        "failed",
                        "effect",
                        1,
                        "1",
                        "2",
                        "record",
                        Instant.now(),
                        "trace",
                        json.valueToTree(Map.of("id", 1)));
        assertThatThrownBy(() -> inbox.receive(failed)).isInstanceOf(RuntimeException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM platform_inbox WHERE event_id='failed'",
                                Integer.class))
                .isZero();
    }
}
