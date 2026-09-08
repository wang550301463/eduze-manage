package com.eduze.engagement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class MySqlEngagementIT {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Test
    void mysqlPersistsPublishedLeadAndActivityHistoryWithoutLeakingOtherAccounts() {
        var ds =
                new DriverManagerDataSource(
                        MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__engagement.sql"))
                .execute(ds);
        var jdbc = new JdbcTemplate(ds);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        var service = new EngagementService(jdbc, mock(InternalClient.class));
        var operations = new EngagementOperations(jdbc);
        var staff =
                new Actor(
                        "staff", "1", Set.of("b"), Set.of("PRINCIPAL"), Set.of("engagement:write"));
        var parent = new Actor("p", "1", Set.of(), Set.of("PARENT"), Set.of());
        tx.execute(
                s ->
                        service.saveStudio(
                                staff,
                                new EngagementModels.StudioInput(
                                        "b", "画室", "介绍", "地址", "13800000000", true)));
        var enquiry =
                tx.execute(
                        s ->
                                service.enquire(
                                        "b",
                                        new EngagementModels.EnquiryInput(
                                                "学生", "13800000001", 8, "展览", "e", "", "")));
        tx.execute(
                s -> {
                    service.followup(
                            staff,
                            enquiry.id(),
                            new EngagementModels.FollowupInput("CONTACTED", "staff", "已联系", null));
                    return null;
                });
        assertThat(service.history(staff, enquiry.id())).hasSize(1);
        var activity =
                tx.execute(
                        s ->
                                service.saveActivity(
                                        staff,
                                        new EngagementModels.ActivityInput(
                                                "b",
                                                "活动",
                                                "介绍",
                                                java.time.Instant.now()
                                                        .plusSeconds(604800)
                                                        .toString(),
                                                1,
                                                true)));
        var signup = tx.execute(s -> service.signup(parent, activity.id()));
        assertThat(operations.signups(parent)).hasSize(1);
        assertThat(
                        operations.signups(
                                new Actor("other", "1", Set.of(), Set.of("PARENT"), Set.of())))
                .isEmpty();
        tx.execute(
                s -> {
                    operations.checkin(staff, signup.id());
                    return null;
                });
        assertThat(operations.signups(parent).get(0).get("status")).isEqualTo("CHECKED_IN");
    }
}
