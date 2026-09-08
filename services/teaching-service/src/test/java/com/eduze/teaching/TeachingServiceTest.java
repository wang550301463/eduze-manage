package com.eduze.teaching;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.Actor;
import com.eduze.platform.runtime.InternalClient;
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

class TeachingServiceTest {
    private TeachingService service;
    private Actor teacher =
            new Actor(
                    "teacher",
                    "1",
                    Set.of("b1"),
                    Set.of("TEACHER"),
                    Set.of("course:read", "course:write"));
    private InternalClient client;

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:teaching"
                                + System.nanoTime()
                                + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__teaching.sql"))
                .execute(ds);
        client = mock(InternalClient.class);
        when(client.post(eq("academic"), anyString(), any(), eq(Map.class)))
                .thenReturn(Map.of("allowed", true));
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class)))
                .thenReturn(Map.of("allowed", true, "branchId", "b1"));
        service =
                new TeachingService(
                        new JdbcTemplate(ds),
                        new ObjectMapper().findAndRegisterModules(),
                        client,
                        mock(com.eduze.platform.runtime.Outbox.class));
    }

    @Test
    void publicationDoesNotChangeWhenDraftChangesAndStaleEditRejected() {
        var input = input("森林", 0);
        var draft = service.createTemplate(teacher, input);
        var published = service.publishTemplate(teacher, draft.id(), 1);
        service.updateTemplate(teacher, draft.id(), input("海洋", 1));
        assertThat(service.templateVersion(teacher, published.id()).content().title())
                .isEqualTo("森林");
        assertThatThrownBy(() -> service.updateTemplate(teacher, draft.id(), input("过期", 1)))
                .hasMessageContaining("版本");
    }

    @Test
    void anotherTeacherCannotReadPrivateDraftButCanReadPublishedVersion() {
        var draft = service.createTemplate(teacher, input("森林", 0));
        var published = service.publishTemplate(teacher, draft.id(), 1);
        var other = new Actor("other", "1", Set.of("b1"), Set.of("TEACHER"), Set.of("course:read"));
        assertThatThrownBy(() -> service.template(other, draft.id())).hasMessageContaining("权限");
        assertThat(service.templateVersion(other, published.id()).content().title())
                .isEqualTo("森林");
    }

    @Test
    void classesExtendAndFinishIndependently() {
        var draft = service.createTemplate(teacher, input("森林", 0));
        var version = service.publishTemplate(teacher, draft.id(), 1);
        var first =
                service.createTheme(
                        teacher,
                        new TeachingModels.ThemeInput(
                                0, "b1", "g1", version.id(), "一班", List.of("l1", "l2", "l3"), ""));
        var second =
                service.createTheme(
                        teacher,
                        new TeachingModels.ThemeInput(
                                0, "b1", "g2", version.id(), "二班", List.of("l4", "l5", "l6"), ""));
        service.updateTheme(
                teacher,
                first.id(),
                new TeachingModels.ThemeInput(
                        1,
                        "b1",
                        "g1",
                        version.id(),
                        "一班",
                        List.of("l1", "l2", "l3", "l7"),
                        "补充细节"));
        service.transition(teacher, first.id(), 2, "FINISHED", "已完成");
        assertThat(service.theme(teacher, second.id()).status()).isEqualTo("PLANNED");
        assertThat(service.theme(teacher, first.id()).content().lessonIds()).hasSize(4);
    }

    @Test
    void unavailableAcademicAuthorizationRejectsExistingThemeRead() {
        var draft = service.createTemplate(teacher, input("森林", 0));
        var version = service.publishTemplate(teacher, draft.id(), 1);
        var theme =
                service.createTheme(
                        teacher,
                        new TeachingModels.ThemeInput(
                                0, "b1", "g1", version.id(), "一班", List.of("l1"), ""));
        when(client.get(eq("academic"), endsWith("/access"), eq(Map.class))).thenReturn(null);
        assertThatThrownBy(() -> service.theme(teacher, theme.id())).hasMessageContaining("权限");
    }

    @Test
    void themesListDoesNotExposeAnotherTeachersClass() {
        var template = service.createTemplate(teacher, input("森林", 0));
        var published = service.publishTemplate(teacher, template.id(), 1);
        service.createTheme(
                teacher,
                new TeachingModels.ThemeInput(
                        0, "b1", "g1", published.id(), "一班", List.of("l1"), "交接"));
        when(client.post(eq("academic"), endsWith("/batch-access"), any(), eq(List.class)))
                .thenReturn(List.of(Map.of("id", "g1", "allowed", false)));
        assertThat(service.themes(teacher, "b1", null, 100)).isEmpty();
    }

    private TeachingModels.TemplateInput input(String title, int version) {
        return new TeachingModels.TemplateInput(
                version,
                title,
                5,
                8,
                "表达",
                "彩笔",
                3,
                List.of(new TeachingModels.Step("构图", "观察后画草图")),
                List.of("建筑"),
                List.of());
    }
}
