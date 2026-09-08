package com.eduze.portfolio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.Actor;
import com.eduze.platform.runtime.InternalClient;
import com.eduze.platform.runtime.Outbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class PortfolioServiceTest {
    private PortfolioService service;
    private Actor teacher =
            new Actor(
                    "t",
                    "1",
                    Set.of("b1"),
                    Set.of("TEACHER"),
                    Set.of("student:read", "student:write"));
    private Actor parent = new Actor("p", "1", Set.of(), Set.of("PARENT"), Set.of());
    private InternalClient client;

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:portfolio"
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
        when(client.get(eq("teaching"), anyString(), eq(Map.class)))
                .thenReturn(Map.of("content", Map.of("branchId", "b1", "groupId", "g1")));
        when(client.get(eq("academic"), endsWith("/students"), eq(List.class)))
                .thenReturn(List.of(Map.of("id", "s1", "name", "学生", "branchId", "b1")));
        service =
                new PortfolioService(
                        new JdbcTemplate(ds),
                        new ObjectMapper().findAndRegisterModules(),
                        client,
                        mock(Outbox.class));
    }

    @Test
    void householdSeesImmutablePublicationAndWithdrawalStopsRead() {
        var draft = service.create(teacher, input("第一版", 0));
        var publication = service.publish(teacher, draft.id(), 1, "key1");
        assertThat(service.publish(teacher, draft.id(), 1, "key1").id())
                .isEqualTo(publication.id());
        var updated = service.update(teacher, draft.id(), input("尚未发布", 2));
        assertThat(service.record(parent, draft.id()).content().comment()).isEqualTo("第一版");
        assertThatThrownBy(() -> service.update(teacher, draft.id(), input("过期编辑", 2)))
                .hasMessageContaining("版本");
        service.withdraw(teacher, draft.id(), updated.version(), "照片归属修正");
        assertThatThrownBy(() -> service.record(parent, draft.id())).hasMessageContaining("发布");
    }

    @Test
    void revokedFamilyRelationshipIsCheckedOnEveryRead() {
        var draft = service.create(teacher, input("观察", 0));
        service.publish(teacher, draft.id(), 1, "key1");
        assertThat(service.record(parent, draft.id()).id()).isEqualTo(draft.id());
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class)))
                .thenReturn(Map.of("allowed", false));
        assertThatThrownBy(() -> service.record(parent, draft.id())).hasMessageContaining("权限");
    }

    @Test
    void studentsMayCompleteWithoutFinishedArtwork() {
        var draft = service.create(teacher, input("实验观察", 0));
        assertThat(service.publish(teacher, draft.id(), 1, "key1").content().artworks()).isEmpty();
    }

    @Test
    void rejectsStudentOutsideTheThemeClass() {
        when(client.get(eq("academic"), endsWith("/students"), eq(List.class)))
                .thenReturn(List.of(Map.of("id", "other")));
        assertThatThrownBy(() -> service.create(teacher, input("不在班级", 0)))
                .hasMessageContaining("班级");
    }

    private PortfolioModels.DraftInput input(String comment, int version) {
        return new PortfolioModels.DraftInput(
                version, "theme1", "s1", "COMPLETED", "色彩探索", comment, List.of(), List.of());
    }
}
