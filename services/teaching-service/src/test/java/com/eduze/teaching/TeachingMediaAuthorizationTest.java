package com.eduze.teaching;

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

class TeachingMediaAuthorizationTest {
    TeachingService service;
    JdbcTemplate jdbc;
    InternalClient client;
    Actor author =
            new Actor(
                    "author",
                    "1",
                    Set.of("b"),
                    Set.of("TEACHER"),
                    Set.of("course:read", "teaching:write"));
    Actor colleague =
            new Actor(
                    "other",
                    "1",
                    Set.of("b"),
                    Set.of("TEACHER"),
                    Set.of("course:read", "teaching:write"));

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:teachingmedia"
                                + System.nanoTime()
                                + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/V1__teaching.sql"),
                        new ClassPathResource("db/runtime/V0__runtime.sql"))
                .execute(ds);
        jdbc = new JdbcTemplate(ds);
        client = mock(InternalClient.class);
        var json = new ObjectMapper().findAndRegisterModules();
        when(client.get(eq("media"), anyString(), eq(Map.class)))
                .thenReturn(Map.of("usable", true, "tenantId", "1", "ownerId", "author"));
        service = new TeachingService(jdbc, json, client, new Outbox(jdbc, json));
    }

    @Test
    void knowingAnotherTeachersMediaIdCannotCreateReadableDraft() {
        assertThatThrownBy(() -> service.createTemplate(colleague, input(0)))
                .isInstanceOfSatisfying(
                        PlatformException.class, e -> assertThat(e.status()).isEqualTo(403));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM teaching_template", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_outbox", Integer.class))
                .isZero();
    }

    @Test
    void publishedSourceCanBeExplicitlyCopiedAndLaterEditedWithoutUploaderOwnership() {
        var draft = service.createTemplate(author, input(0));
        var published = service.publishTemplate(author, draft.id(), 1);
        var copy = service.copyTemplate(colleague, published.id());
        assertThat(copy.ownerId()).isEqualTo("other");
        assertThat(service.updateTemplate(colleague, copy.id(), input(1)).content().mediaIds())
                .containsExactly("private-media");
        assertThat(service.publishTemplate(colleague, copy.id(), 2).content().mediaIds())
                .containsExactly("private-media");
    }

    @Test
    void privateResourceCannotBeCopiedButPublishedResourceCan() {
        var resource =
                service.createResource(
                        author,
                        new TeachingModels.ResourceInput(
                                0, "课件", "IMAGE", List.of("private-media"), List.of(), "描述"));
        assertThatThrownBy(() -> service.copyResource(colleague, resource.id()))
                .isInstanceOfSatisfying(
                        PlatformException.class, e -> assertThat(e.status()).isEqualTo(403));
        service.publishResource(author, resource.id(), 1);
        var copy = service.copyResource(colleague, resource.id());
        assertThat(copy.ownerId()).isEqualTo("other");
        assertThat(copy.published()).isFalse();
    }

    TeachingModels.TemplateInput input(int version) {
        return new TeachingModels.TemplateInput(
                version,
                "主题",
                5,
                8,
                "目标",
                "材料",
                2,
                List.of(new TeachingModels.Step("观察", "过程")),
                List.of(),
                List.of("private-media"));
    }
}
