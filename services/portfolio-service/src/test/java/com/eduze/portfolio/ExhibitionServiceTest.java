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
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class ExhibitionServiceTest {
    private ExhibitionService exhibitions;
    private PortfolioService portfolios;
    private Actor teacher =
            new Actor(
                    "t",
                    "1",
                    Set.of("b1"),
                    Set.of("TEACHER"),
                    Set.of("student:read", "student:write"));
    private Actor principal =
            new Actor(
                    "boss",
                    "1",
                    Set.of("b1"),
                    Set.of("PRINCIPAL"),
                    Set.of("student:read", "student:write", "exhibition:approve"));
    private Actor parent = new Actor("p", "1", Set.of(), Set.of("PARENT"), Set.of());
    private InternalClient client;

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:exhibition"
                                + System.nanoTime()
                                + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/V1__portfolio.sql"),
                        new ClassPathResource(
                                "db/migration/V2__entries_and_curated_collections.sql"))
                .execute(ds);
        client = mock(InternalClient.class);
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class)))
                .thenReturn(Map.of("allowed", true, "branchId", "b1"));
        when(client.get(eq("academic"), endsWith("/recipients"), eq(Map.class)))
                .thenReturn(
                        Map.of(
                                "tenantId",
                                "1",
                                "branchId",
                                "b1",
                                "recipients",
                                List.of(Map.of("userId", "p"))));
        when(client.get(eq("teaching"), anyString(), eq(Map.class)))
                .thenReturn(Map.of("content", Map.of("branchId", "b1", "groupId", "g1")));
        when(client.get(eq("media"), anyString(), eq(Map.class)))
                .thenReturn(Map.of("usable", true, "tenantId", "1", "ownerId", "t"));
        when(client.get(eq("academic"), endsWith("/students"), eq(List.class)))
                .thenReturn(List.of(Map.of("id", "s1", "name", "学生", "branchId", "b1")));
        var json = new ObjectMapper().findAndRegisterModules();
        var jdbc = new JdbcTemplate(ds);
        portfolios = new PortfolioService(jdbc, json, client, mock(Outbox.class));
        exhibitions = new ExhibitionService(jdbc, json, client, portfolios);
    }

    @Test
    void publicPublicationNeedsGuardianConsentAndApprovalAndStopsOnRevocation() {
        var record =
                portfolios.create(
                        teacher,
                        new PortfolioModels.DraftInput(
                                0,
                                "theme1",
                                "s1",
                                "COMPLETED",
                                "课堂",
                                "私密个性评价",
                                List.of(
                                        new PortfolioModels.Artwork(
                                                "w1",
                                                "森林",
                                                "FINAL",
                                                List.of("m1"),
                                                List.of("s1"),
                                                "我的故事")),
                                List.of()));
        var publication = portfolios.publish(teacher, record.id(), 1, "publish1");
        var exhibition =
                exhibitions.create(
                        teacher,
                        new PortfolioModels.ExhibitionInput(
                                "森林展", "作品分享", List.of(publication.id())));
        assertThatThrownBy(() -> exhibitions.approve(principal, exhibition.id()))
                .hasMessageContaining("授权");
        assertThatThrownBy(
                        () ->
                                exhibitions.consent(
                                        teacher,
                                        publication.id(),
                                        new PortfolioModels.Consent(true, "老师代办")))
                .hasMessageContaining("监护人");
        exhibitions.consent(parent, publication.id(), new PortfolioModels.Consent(true, "小画家"));
        exhibitions.approve(principal, exhibition.id());
        assertThat(exhibitions.publicDetail(exhibition.id()).toString())
                .doesNotContain("私密个性评价")
                .doesNotContain("studentId");
        assertThat((List<?>) exhibitions.publicDetail(exhibition.id()).get("works")).hasSize(1);
        exhibitions.consent(parent, publication.id(), new PortfolioModels.Consent(false, "小画家"));
        assertThat((List<?>) exhibitions.publicDetail(exhibition.id()).get("works")).isEmpty();
    }

    @Test
    void withdrawalThenReissueDoesNotResurrectOldPublicVersion() {
        var record =
                portfolios.create(
                        teacher,
                        new PortfolioModels.DraftInput(
                                0, "theme1", "s1", "COMPLETED", "课堂", "旧版", List.of(), List.of()));
        var publication = portfolios.publish(teacher, record.id(), 1, "publish1");
        var exhibition =
                exhibitions.create(
                        teacher,
                        new PortfolioModels.ExhibitionInput(
                                "森林展", "作品分享", List.of(publication.id())));
        exhibitions.consent(parent, publication.id(), new PortfolioModels.Consent(true, "小画家"));
        exhibitions.approve(principal, exhibition.id());
        portfolios.withdraw(teacher, record.id(), 2, "错误照片");
        portfolios.publish(teacher, record.id(), 3, "publish2");
        assertThat((List<?>) exhibitions.publicDetail(exhibition.id()).get("works")).isEmpty();
    }

    @Test
    void revokedRelationshipStopsPublicReadsEvenBeforeAnEventArrives() {
        var record =
                portfolios.create(
                        teacher,
                        new PortfolioModels.DraftInput(
                                0, "theme1", "s1", "COMPLETED", "课堂", "评价", List.of(), List.of()));
        var publication = portfolios.publish(teacher, record.id(), 1, "publish1");
        var exhibition =
                exhibitions.create(
                        teacher,
                        new PortfolioModels.ExhibitionInput(
                                "森林展", "作品分享", List.of(publication.id())));
        exhibitions.consent(parent, publication.id(), new PortfolioModels.Consent(true, "小画家"));
        exhibitions.approve(principal, exhibition.id());
        when(client.get(eq("academic"), endsWith("/recipients"), eq(Map.class)))
                .thenReturn(Map.of("tenantId", "1", "branchId", "b1", "recipients", List.of()));
        assertThat((List<?>) exhibitions.publicDetail(exhibition.id()).get("works")).isEmpty();
    }
}
