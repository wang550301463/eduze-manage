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

class PortfolioMediaAuthorizationTest {
    PortfolioService service;
    JdbcTemplate jdbc;
    InternalClient client;
    Actor author =
            new Actor(
                    "author",
                    "1",
                    Set.of("b"),
                    Set.of("TEACHER"),
                    Set.of("student:read", "portfolio:write"));
    Actor substitute =
            new Actor(
                    "substitute",
                    "1",
                    Set.of("b"),
                    Set.of("TEACHER"),
                    Set.of("student:read", "portfolio:write"));

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:portfoliomedia"
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
        client = mock(InternalClient.class);
        var json = new ObjectMapper().findAndRegisterModules();
        when(client.get(eq("media"), anyString(), eq(Map.class)))
                .thenReturn(Map.of("usable", true, "tenantId", "1", "ownerId", "author"));
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class)))
                .thenReturn(Map.of("allowed", true, "branchId", "b"));
        when(client.get(eq("academic"), endsWith("/students"), eq(List.class)))
                .thenReturn(List.of(Map.of("id", "s")));
        when(client.get(eq("teaching"), anyString(), eq(Map.class)))
                .thenReturn(
                        Map.of(
                                "content",
                                Map.of(
                                        "branchId",
                                        "b",
                                        "groupId",
                                        "g",
                                        "lessonIds",
                                        List.of("l1"))));
        service = new PortfolioService(jdbc, json, client, new Outbox(jdbc, json));
    }

    @Test
    void anotherTeachersPrivateMediaCannotBeAttachedByKnowingId() {
        assertThatThrownBy(() -> service.create(substitute, input("theme", 0)))
                .isInstanceOfSatisfying(
                        PlatformException.class, e -> assertThat(e.status()).isEqualTo(403));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM portfolio_record", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_outbox", Integer.class))
                .isZero();
    }

    @Test
    void substituteCanRetainAuthorizedExistingMediaButCannotInjectForeignExtraFile() {
        var record = service.create(author, input("theme", 0));
        assertThat(service.update(substitute, record.id(), input("theme", 1)).content().artworks())
                .hasSize(1);
        assertThat(service.publish(substitute, record.id(), 2, "publish").content().artworks())
                .hasSize(1);
        var foreign =
                new PortfolioModels.DraftInput(
                        3,
                        "theme",
                        "s",
                        "COMPLETED",
                        "课堂",
                        "评价",
                        List.of(
                                new PortfolioModels.Artwork(
                                        "w",
                                        "作品",
                                        "FINAL",
                                        List.of("new-private-file"),
                                        List.of("s"),
                                        "")),
                        List.of());
        assertThatThrownBy(() -> service.update(substitute, record.id(), foreign))
                .isInstanceOfSatisfying(
                        PlatformException.class, e -> assertThat(e.status()).isEqualTo(403));
    }

    @Test
    void explicitSourceReuseRequiresLiveSourceStudentAuthorization() {
        var source = service.create(author, input("theme", 0));
        assertThat(
                        service.create(substitute, input("another-theme", 0), source.id())
                                .content()
                                .artworks())
                .hasSize(1);
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class)))
                .thenReturn(Map.of("allowed", false));
        assertThatThrownBy(() -> service.create(substitute, input("third-theme", 0), source.id()))
                .isInstanceOf(PlatformException.class);
    }

    @Test
    void explicitInaccessibleSourceIsRejectedEvenWhenDestinationStudentIsAllowed() {
        when(client.get(eq("academic"), endsWith("/students"), eq(List.class)))
                .thenReturn(List.of(Map.of("id", "s"), Map.of("id", "private-child")));
        var sourceInput =
                new PortfolioModels.DraftInput(
                        0,
                        "source-theme",
                        "private-child",
                        "COMPLETED",
                        "课堂",
                        "评价",
                        List.of(
                                new PortfolioModels.Artwork(
                                        "w",
                                        "作品",
                                        "FINAL",
                                        List.of("private-media"),
                                        List.of("private-child"),
                                        "")),
                        List.of());
        var source = service.create(author, sourceInput);
        when(client.get(
                        eq("academic"),
                        eq("/internal/academic/students/private-child/access"),
                        eq(Map.class)))
                .thenReturn(Map.of("allowed", false));
        assertThatThrownBy(() -> service.create(substitute, input("destination", 0), source.id()))
                .isInstanceOfSatisfying(
                        PlatformException.class, e -> assertThat(e.status()).isEqualTo(403));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM portfolio_record", Integer.class))
                .isEqualTo(1);
    }

    PortfolioModels.DraftInput input(String theme, int version) {
        return new PortfolioModels.DraftInput(
                version,
                theme,
                "s",
                "COMPLETED",
                "课堂",
                "评价",
                List.of(
                        new PortfolioModels.Artwork(
                                "w", "作品", "FINAL", List.of("private-media"), List.of("s"), "")),
                List.of());
    }
}
