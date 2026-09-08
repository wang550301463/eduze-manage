package com.eduze.portfolio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class ExhibitionReviewTest {
    PortfolioService portfolios;
    ExhibitionService exhibitions;
    InternalClient client;
    Actor teacher =
            new Actor(
                    "t",
                    "1",
                    Set.of("b"),
                    Set.of("TEACHER"),
                    Set.of("student:read", "portfolio:write"));
    Actor principal =
            new Actor(
                    "boss",
                    "1",
                    Set.of("b"),
                    Set.of("PRINCIPAL"),
                    Set.of("student:read", "portfolio:write", "exhibition:approve"));
    Actor first = new Actor("p", "1", Set.of(), Set.of("PARENT"), Set.of());
    Actor second = new Actor("q", "1", Set.of(), Set.of("PARENT"), Set.of());

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:exreview"
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
        var jdbc = new JdbcTemplate(ds);
        var json = new ObjectMapper().findAndRegisterModules();
        client = mock(InternalClient.class);
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class)))
                .thenReturn(Map.of("allowed", true, "branchId", "b"));
        when(client.get(eq("academic"), endsWith("/students"), eq(List.class)))
                .thenReturn(List.of(Map.of("id", "s1"), Map.of("id", "s2")));
        when(client.get(eq("academic"), endsWith("/recipients"), eq(Map.class)))
                .thenAnswer(
                        call ->
                                Map.of(
                                        "tenantId",
                                        "1",
                                        "recipients",
                                        List.of(
                                                Map.of(
                                                        "userId",
                                                        call.<String>getArgument(1).contains("s1")
                                                                ? "p"
                                                                : "q"))));
        when(client.get(eq("teaching"), anyString(), eq(Map.class)))
                .thenReturn(Map.of("content", Map.of("branchId", "b", "groupId", "g")));
        when(client.get(eq("media"), anyString(), eq(Map.class)))
                .thenReturn(Map.of("usable", true, "tenantId", "1", "ownerId", "t"));
        portfolios = new PortfolioService(jdbc, json, client, new Outbox(jdbc, json));
        exhibitions = new ExhibitionService(jdbc, json, client, portfolios);
    }

    @Test
    void anotherCampusPrincipalCannotWithdrawOrArchiveAnExhibition() {
        var publication = publication();
        var exhibit =
                exhibitions.create(
                        teacher,
                        new PortfolioModels.ExhibitionInput("展览", "介绍", List.of(publication.id())));
        exhibitions.consent(first, publication.id(), new PortfolioModels.Consent(true, "画家"));
        exhibitions.consent(second, publication.id(), new PortfolioModels.Consent(true, "伙伴"));
        exhibitions.approve(principal, exhibit.id());
        Actor foreign =
                new Actor(
                        "other-boss",
                        "1",
                        Set.of("other-branch"),
                        Set.of("PRINCIPAL"),
                        Set.of("student:read", "portfolio:write", "exhibition:approve"));
        assertThatThrownBy(() -> exhibitions.withdraw(foreign, exhibit.id()))
                .isInstanceOf(PlatformException.class);
        assertThatThrownBy(() -> exhibitions.archive(foreign, exhibit.id()))
                .isInstanceOf(PlatformException.class);
        assertThat(exhibitions.publicDetail(exhibit.id()).get("exhibition").toString())
                .contains("PUBLISHED");
    }

    @Test
    void allCollaboratorsConsentIsRequiredAndCertificatesEscapeMarkup() {
        var publication = publication();
        var exhibit =
                exhibitions.create(
                        teacher,
                        new PortfolioModels.ExhibitionInput(
                                "<script>展览", "介绍", List.of(publication.id())));
        exhibitions.consent(first, publication.id(), new PortfolioModels.Consent(true, "<小画家>"));
        assertThatThrownBy(() -> exhibitions.approve(principal, exhibit.id()))
                .hasMessageContaining("授权");
        exhibitions.consent(second, publication.id(), new PortfolioModels.Consent(true, "小伙伴"));
        exhibitions.approve(principal, exhibit.id());
        assertThat(exhibitions.certificate(exhibit.id(), publication.id()).get("svg"))
                .contains("&lt;小画家&gt;")
                .doesNotContain("<script>");
        assertThat(exhibitions.shareCard(exhibit.id()).get("svg")).contains("&lt;script&gt;展览");
        exhibitions.archive(principal, exhibit.id());
        assertThat(exhibitions.publicDetail(exhibit.id()).get("exhibition").toString())
                .contains("ARCHIVED");
        exhibitions.consent(second, publication.id(), new PortfolioModels.Consent(false, "小伙伴"));
        assertThatThrownBy(() -> exhibitions.certificate(exhibit.id(), publication.id()))
                .isInstanceOf(PlatformException.class);
    }

    @Test
    void scheduledExhibitionDoesNotLeakBeforeStartAndCollectionsRemainPrivate() {
        var publication = publication();
        var exhibit =
                exhibitions.create(
                        teacher,
                        new PortfolioModels.ExhibitionInput(
                                "未来展",
                                "介绍",
                                List.of(publication.id()),
                                Instant.now().plusSeconds(3600),
                                Instant.now().plusSeconds(7200)));
        exhibitions.consent(first, publication.id(), new PortfolioModels.Consent(true, "甲"));
        exhibitions.consent(second, publication.id(), new PortfolioModels.Consent(true, "乙"));
        exhibitions.approve(principal, exhibit.id());
        assertThat(exhibitions.publicList()).isEmpty();
        assertThatThrownBy(() -> exhibitions.publicDetail(exhibit.id()))
                .isInstanceOf(PlatformException.class);
        var collection =
                portfolios.createCollection(
                        first,
                        "s1",
                        new PortfolioModels.ReportInput("精选", "成长", List.of(publication.id())));
        assertThat(portfolios.collections(first, "s1")).hasSize(1);
        assertThat(collection.publications().get(0).content().artworks().get(0).participantIds())
                .containsExactly("s1");
        assertThatThrownBy(
                        () ->
                                exhibitions.consent(
                                        new Actor(
                                                "p", "1", Set.of("b"), Set.of("TEACHER"), Set.of()),
                                        publication.id(),
                                        new PortfolioModels.Consent(true, "不能代办")))
                .hasMessageContaining("监护人");
    }

    PortfolioModels.Publication publication() {
        var draft =
                portfolios.create(
                        teacher,
                        new PortfolioModels.DraftInput(
                                0,
                                "theme",
                                "s1",
                                "COMPLETED",
                                "课堂",
                                "私人反馈",
                                List.of(
                                        new PortfolioModels.Artwork(
                                                "w",
                                                "合作",
                                                "FINAL",
                                                List.of("m"),
                                                List.of("s1", "s2"),
                                                "创作")),
                                List.of()));
        return portfolios.publish(teacher, draft.id(), 1, "publish");
    }
}
