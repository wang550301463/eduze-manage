package com.eduze.portfolio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PortfolioMySqlIT {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Test
    void concurrentPublicationCommitsExactlyOneSnapshotAndOutboxEvent() throws Exception {
        var ds =
                new DriverManagerDataSource(
                        MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/runtime", "classpath:db/migration")
                .load()
                .migrate();
        var jdbc = new JdbcTemplate(ds);
        var json = new ObjectMapper().findAndRegisterModules();
        var client = mock(InternalClient.class);
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class)))
                .thenReturn(Map.of("allowed", true, "branchId", "b1"));
        when(client.get(eq("academic"), endsWith("/students"), eq(List.class)))
                .thenReturn(List.of(Map.of("id", "s1")));
        when(client.get(eq("teaching"), anyString(), eq(Map.class)))
                .thenReturn(Map.of("content", Map.of("branchId", "b1", "groupId", "g1")));
        var service = new PortfolioService(jdbc, json, client, new Outbox(jdbc, json));
        var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        Actor actor =
                new Actor(
                        "teacher",
                        "1",
                        Set.of("b1"),
                        Set.of("TEACHER"),
                        Set.of("student:read", "portfolio:write"));
        var record =
                tx.execute(
                        s ->
                                service.create(
                                        actor,
                                        new PortfolioModels.DraftInput(
                                                0,
                                                "theme",
                                                "s1",
                                                "COMPLETED",
                                                "观察",
                                                "创作",
                                                List.of(),
                                                List.of())));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<String> publish =
                    () -> {
                        start.await();
                        return tx.execute(
                                        s -> service.publish(actor, record.id(), 1, "same-command"))
                                .id();
                    };
            Future<String> first = executor.submit(publish);
            Future<String> second = executor.submit(publish);
            start.countDown();
            assertThat(first.get(20, TimeUnit.SECONDS)).isEqualTo(second.get(20, TimeUnit.SECONDS));
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM portfolio_publication", Integer.class))
                    .isEqualTo(1);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM platform_outbox WHERE target='notification'",
                                    Integer.class))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
