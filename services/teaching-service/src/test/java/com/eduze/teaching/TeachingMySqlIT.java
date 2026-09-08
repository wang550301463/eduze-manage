package com.eduze.teaching;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class TeachingMySqlIT {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Test
    void serviceMigrationsAndPublishedSnapshotsWorkOnMySql() {
        var ds =
                new DriverManagerDataSource(
                        MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/runtime", "classpath:db/migration")
                .load()
                .migrate();
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        var client = mock(InternalClient.class);
        var service =
                new TeachingService(
                        jdbc,
                        new ObjectMapper().findAndRegisterModules(),
                        client,
                        new Outbox(jdbc, new ObjectMapper().findAndRegisterModules()));
        Actor actor =
                new Actor(
                        "teacher",
                        "1",
                        Set.of("b1"),
                        Set.of("TEACHER"),
                        Set.of("course:read", "teaching:write"));
        var draft =
                service.createTemplate(
                        actor,
                        new TeachingModels.TemplateInput(
                                0,
                                "森林",
                                5,
                                9,
                                "表达",
                                "水彩",
                                3,
                                List.of(new TeachingModels.Step("草图", "观察树木")),
                                List.of("自然"),
                                List.of()));
        var published = service.publishTemplate(actor, draft.id(), 1);
        assertThat(service.templateVersion(actor, published.id()).content().title())
                .isEqualTo("森林");
        assertThat(new CurriculumService(jdbc).stages(actor)).hasSize(5);
        assertThat(new CurriculumService(jdbc).dimensions(actor, null)).hasSize(22);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='t_student'",
                                Integer.class))
                .isZero();
    }
}
