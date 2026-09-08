package com.eduze.portfolio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class PortfolioReviewTest {
    JdbcTemplate jdbc;
    PortfolioService service;
    InternalClient client;
    Actor teacher =
            new Actor(
                    "t",
                    "1",
                    Set.of("b"),
                    Set.of("TEACHER"),
                    Set.of("student:read", "portfolio:write"));
    Actor parent = new Actor("p", "1", Set.of(), Set.of("PARENT"), Set.of());
    ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:review" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
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
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class)))
                .thenReturn(Map.of("allowed", true, "branchId", "b"));
        when(client.get(eq("academic"), endsWith("/students"), eq(List.class)))
                .thenReturn(List.of(Map.of("id", "s", "name", "学生")));
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
                                        List.of("l1", "l2", "l3", "l4"))));
        when(client.get(eq("media"), anyString(), eq(Map.class)))
                .thenReturn(Map.of("usable", true, "tenantId", "1", "ownerId", "t"));
        when(client.post(eq("academic"), endsWith("batch-access"), any(), eq(List.class)))
                .thenAnswer(
                        call ->
                                ((List<?>) ((Map<?, ?>) call.getArgument(2)).get("ids"))
                                        .stream()
                                                .map(
                                                        id ->
                                                                Map.of(
                                                                        "id",
                                                                        id,
                                                                        "allowed",
                                                                        id.toString()
                                                                                .startsWith("s")))
                                                .toList());
        when(client.post(eq("media"), eq("/internal/media/access"), any(), eq(Map.class)))
                .thenReturn(Map.of("items", List.of(Map.of("id", "m1", "url", "signed"))));
        service = new PortfolioService(jdbc, json, client, new Outbox(jdbc, json));
    }

    @Test
    void limitAppliesAfterAuthorizationAndRosterDoesNotCapExistingRecords() throws Exception {
        for (int i = 0; i < 205; i++) {
            String student = i < 105 ? "s" + i : "denied" + i;
            var content = draft(student, "m1", 0);
            jdbc.update(
                    "INSERT INTO portfolio_record(id,tenant_id,branch_id,theme_id,student_id,row_version,progress_status,publication_status,content_json,created_at) VALUES (?, '1','b','theme',?,1,'COMPLETED','DRAFT',?,?)",
                    "r" + i,
                    student,
                    json.writeValueAsString(content),
                    Timestamp.from(Instant.now().plusSeconds(i)));
        }
        assertThat(service.records(teacher, null, null, 100)).hasSize(100);
        when(client.get(eq("academic"), endsWith("/students"), eq(List.class)))
                .thenReturn(
                        java.util.stream.IntStream.range(0, 105)
                                .mapToObj(i -> Map.of("id", "s" + i, "name", "学生" + i))
                                .toList());
        assertThat(service.roster(teacher, "theme"))
                .hasSize(105)
                .allSatisfy(row -> assertThat(row.get("recordId")).isNotNull());
    }

    @Test
    void editedPublicationRemainsVisibleButShowsPendingAndHistoricalMediaWorks() {
        var record = service.create(teacher, draft("s", "m1", 0));
        var first = service.publish(teacher, record.id(), 1, "first");
        var report =
                service.report(
                        teacher,
                        "s",
                        new PortfolioModels.ReportInput("学期", "总结", List.of(first.id())));
        service.update(teacher, record.id(), draft("s", "m2", 2));
        assertThat(service.record(teacher, record.id()).needsPublishing()).isTrue();
        assertThat(service.dashboard(teacher, "b").get("needsPublishing")).isEqualTo(1);
        service.publish(teacher, record.id(), 3, "second");
        assertThat(service.record(teacher, record.id()).needsPublishing()).isFalse();
        assertThat(service.publicationMedia(parent, first.id(), List.of("m1"))).isNotNull();
        assertThat(service.reportMedia(parent, report.id(), first.id(), List.of("m1"))).isNotNull();
        assertThatThrownBy(() -> service.publicationMedia(parent, first.id(), List.of("m2")))
                .isInstanceOf(PlatformException.class);
        service.withdraw(teacher, record.id(), 4, "撤回");
        assertThatThrownBy(() -> service.publicationMedia(parent, first.id(), List.of("m1")))
                .isInstanceOf(PlatformException.class);
    }

    @Test
    void perLessonEntriesAreAppendOnlyAndExplicitlyPublished() {
        var record = service.create(teacher, draft("s", "m1", 0));
        var first =
                service.addEntry(
                        teacher,
                        record.id(),
                        new PortfolioModels.EntryInput(
                                "l1", Instant.now(), "构图", List.of("m1"), "e1"));
        service.addEntry(
                teacher,
                record.id(),
                new PortfolioModels.EntryInput("l4", Instant.now(), "补课", List.of("m2"), "e4"));
        assertThat(service.entries(parent, record.id())).isEmpty();
        service.publishEntry(teacher, record.id(), first.id(), "p1");
        assertThat(service.entries(parent, record.id()))
                .hasSize(1)
                .first()
                .extracting(PortfolioModels.Entry::notes)
                .isEqualTo("构图");
        assertThat(service.entries(teacher, record.id())).hasSize(2);
        assertThatThrownBy(
                        () ->
                                service.addEntry(
                                        teacher,
                                        record.id(),
                                        new PortfolioModels.EntryInput(
                                                "unrelated",
                                                Instant.now(),
                                                "非法",
                                                List.of(),
                                                "bad")))
                .hasMessageContaining("课次");
    }

    @Test
    void mediaReferencesAreTransactionalEventsAndNoRemoteMutationOccurs() {
        service.create(teacher, draft("s", "m1", 0));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM platform_outbox WHERE target='media'",
                                Integer.class))
                .isEqualTo(1);
        verify(client, never()).post(eq("media"), eq("/internal/media/references"), any(), any());
    }

    @Test
    void rejectsMoreThanOneHundredUniqueMediaAcrossArtworksAndAudioWithoutMutation() {
        List<String> images =
                java.util.stream.IntStream.range(0, 100).mapToObj(i -> "media" + i).toList();
        var oversized =
                new PortfolioModels.DraftInput(
                        0,
                        "theme",
                        "s",
                        "COMPLETED",
                        "课堂",
                        "评价",
                        List.of(
                                new PortfolioModels.Artwork(
                                        "w1",
                                        "上半",
                                        "FINAL",
                                        images.subList(0, 50),
                                        List.of("s"),
                                        ""),
                                new PortfolioModels.Artwork(
                                        "w2",
                                        "下半",
                                        "FINAL",
                                        images.subList(50, 100),
                                        List.of("s"),
                                        "")),
                        List.of("audio"));
        assertThatThrownBy(() -> service.create(teacher, oversized))
                .isInstanceOfSatisfying(
                        PlatformException.class, error -> assertThat(error.status()).isEqualTo(400))
                .hasMessageContaining("100");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM portfolio_record", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_outbox", Integer.class))
                .isZero();
    }

    @Test
    void historicalRosterAfterTransferKeepsActualNeedsPublishingState() {
        var record = service.create(teacher, draft("s", "m1", 0));
        service.publish(teacher, record.id(), 1, "publication");
        when(client.get(eq("academic"), endsWith("/students"), eq(List.class)))
                .thenReturn(List.of());
        assertThat(service.roster(teacher, "theme"))
                .singleElement()
                .satisfies(row -> assertThat(row.get("needsPublishing")).isEqualTo(false));
        service.update(teacher, record.id(), draft("s", "m2", 2));
        assertThat(service.roster(teacher, "theme"))
                .singleElement()
                .satisfies(row -> assertThat(row.get("needsPublishing")).isEqualTo(true));
    }

    PortfolioModels.DraftInput draft(String student, String media, int version) {
        return new PortfolioModels.DraftInput(
                version,
                "theme",
                student,
                "COMPLETED",
                "课堂",
                "评价",
                List.of(
                        new PortfolioModels.Artwork(
                                "w", "作品", "FINAL", List.of(media), List.of(student), "故事")),
                List.of());
    }
}
