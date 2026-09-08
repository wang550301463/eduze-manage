package com.eduze.portfolio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

class PublicationTransactionTest {
    private PortfolioService service;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private InternalClient client;
    private Actor teacher =
            new Actor(
                    "t",
                    "1",
                    Set.of("b1"),
                    Set.of("TEACHER"),
                    Set.of("student:read", "portfolio:write"));

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:publication"
                                + System.nanoTime()
                                + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/V1__portfolio.sql"),
                        new ClassPathResource(
                                "db/migration/V2__entries_and_curated_collections.sql"),
                        new ClassPathResource("db/runtime/V0__runtime.sql"))
                .execute(ds);
        jdbc = new JdbcTemplate(ds);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(ds));
        client = mock(InternalClient.class);
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class)))
                .thenReturn(Map.of("allowed", true, "branchId", "b1"));
        when(client.get(eq("academic"), endsWith("/students"), eq(List.class)))
                .thenReturn(List.of(Map.of("id", "s1", "name", "学生", "branchId", "b1")));
        when(client.get(eq("teaching"), anyString(), eq(Map.class)))
                .thenReturn(Map.of("content", Map.of("branchId", "b1", "groupId", "g1")));
        var json = new ObjectMapper().findAndRegisterModules();
        service = new PortfolioService(jdbc, json, client, new Outbox(jdbc, json));
    }

    @Test
    void publishAndOutboxCommitTogetherAndRepeatEnqueuesOnlyOnce() {
        var record = transactions.execute(s -> service.create(teacher, draft()));
        var publication =
                transactions.execute(s -> service.publish(teacher, record.id(), 1, "once"));
        transactions.execute(s -> service.publish(teacher, record.id(), 1, "once"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM platform_outbox WHERE target='notification'",
                                Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM portfolio_publication", Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT current_publication_id FROM portfolio_record WHERE id=?",
                                String.class,
                                record.id()))
                .isEqualTo(publication.id());
    }

    @Test
    void rolledBackPublicationLeavesNoSnapshotOrEvent() {
        var record = transactions.execute(s -> service.create(teacher, draft()));
        transactions.executeWithoutResult(
                s -> {
                    service.publish(teacher, record.id(), 1, "once");
                    s.setRollbackOnly();
                });
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM platform_outbox WHERE target='notification'",
                                Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM portfolio_publication", Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM platform_outbox WHERE target='media'",
                                Integer.class))
                .isEqualTo(1);
        assertThat(service.record(teacher, record.id()).status()).isEqualTo("DRAFT");
        assertThat(service.record(teacher, record.id()).version()).isEqualTo(1);
    }

    @Test
    void rolledBackNewDraftLeavesNoMediaReferenceEventOrRemoteReferenceMutation() {
        transactions.executeWithoutResult(
                tx -> {
                    service.create(teacher, draft());
                    tx.setRollbackOnly();
                });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM portfolio_record", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_outbox", Integer.class))
                .isZero();
        verify(client, never()).post(eq("media"), eq("/internal/media/references"), any(), any());
    }

    @Test
    void crossTenantCannotReadExistingRecord() {
        var record = transactions.execute(s -> service.create(teacher, draft()));
        var other = new Actor("t2", "2", Set.of("b1"), Set.of("TEACHER"), Set.of("student:read"));
        assertThatThrownBy(() -> service.record(other, record.id()))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("不存在");
    }

    private PortfolioModels.DraftInput draft() {
        return new PortfolioModels.DraftInput(
                0, "theme1", "s1", "COMPLETED", "主题学习", "观察实验", List.of(), List.of());
    }
}
