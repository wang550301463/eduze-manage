package com.eduze.engagement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class EngagementTest {
    private EngagementService service;
    private InternalClient client;
    private JdbcTemplate jdbc;
    private Actor staff =
            new Actor("staff", "1", Set.of("b"), Set.of("PRINCIPAL"), Set.of("engagement:write"));

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:engagement"
                                + System.nanoTime()
                                + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__engagement.sql"))
                .execute(ds);
        client = mock(InternalClient.class);
        jdbc = new JdbcTemplate(ds);
        service = new EngagementService(jdbc, client);
    }

    @Test
    void enquiryRequiresPublishedStudioAndRejectsRepeatedContact() {
        service.saveStudio(
                staff,
                new EngagementModels.StudioInput("b", "画室", "简介", "地址", "13800000000", true));
        var input = new EngagementModels.EnquiryInput("小明", "13800000001", 8, "展览", "展览1", "", "");
        var created = service.enquire("b", input);
        assertThat(created.status()).isEqualTo("NEW");
        assertThatThrownBy(() -> service.enquire("b", input)).isInstanceOf(PlatformException.class);
        assertThatThrownBy(() -> service.enquire("unknown", input))
                .isInstanceOf(PlatformException.class);
    }

    @Test
    void signupIsIdempotentAndDoesNotOverbook() {
        var activity =
                service.saveActivity(
                        staff,
                        new EngagementModels.ActivityInput(
                                "b",
                                "亲子课",
                                "简介",
                                java.time.Instant.now().plusSeconds(604800).toString(),
                                1,
                                true));
        Actor parent = new Actor("p", "1", Set.of(), Set.of("PARENT"), Set.of());
        var first = service.signup(parent, activity.id());
        assertThat(service.signup(parent, activity.id()).id()).isEqualTo(first.id());
        var other = new Actor("p2", "1", Set.of(), Set.of("PARENT"), Set.of());
        assertThatThrownBy(() -> service.signup(other, activity.id())).hasMessageContaining("名额");
        service.cancelSignup(parent, first.id());
        assertThat(service.signup(other, activity.id()).status()).isEqualTo("CONFIRMED");
    }

    @Test
    void enrollmentRequiresAuthoritativeTrialAndExplicitClassSelection() {
        service.saveStudio(
                staff,
                new EngagementModels.StudioInput("b", "画室", "简介", "地址", "13800000000", true));
        var lead =
                service.enquire(
                        "b",
                        new EngagementModels.EnquiryInput(
                                "学生", "13800000003", 8, "试听", "", "", ""));
        assertThatThrownBy(() -> service.enroll(staff, lead.id(), "group"))
                .hasMessageContaining("试听");
        when(client.post(
                        eq("academic"),
                        eq("/internal/academic/trial-reservations"),
                        any(),
                        eq(Map.class)))
                .thenReturn(Map.of("status", "CONFIRMED"));
        service.reserveTrial(staff, lead.id(), new EngagementModels.TrialInput("lesson"));
        when(client.post(eq("academic"), endsWith("/enroll"), any(), eq(Map.class)))
                .thenReturn(Map.of("status", "ENROLLED", "studentId", "student"));
        assertThat(service.enroll(staff, lead.id(), "group").get("status")).isEqualTo("ENROLLED");
        // The lead remains TRIAL_BOOKED until the enrollment event commits locally.
        assertThat(service.enquiries(staff, "b").get(0).status()).isEqualTo("TRIAL_BOOKED");
    }

    @Test
    void authoritativeEnrollmentSurvivesLaterNotesAndRejectsAnotherTrial() {
        service.saveStudio(
                staff,
                new EngagementModels.StudioInput("b", "画室", "简介", "地址", "13800000000", true));
        var lead =
                service.enquire(
                        "b",
                        new EngagementModels.EnquiryInput(
                                "学生", "13800000004", 8, "试听", "", "", ""));
        jdbc.update("UPDATE enquiry SET status='ENROLLED' WHERE id=?", lead.id());
        service.followup(
                staff,
                lead.id(),
                new EngagementModels.FollowupInput("CONTACTED", "staff", "报名后回访", null));
        assertThat(service.enquiries(staff, "b").get(0).status()).isEqualTo("ENROLLED");
        service.followup(
                staff,
                lead.id(),
                new EngagementModels.FollowupInput("ENROLLED", "staff", "家长反馈", null));
        assertThat(service.history(staff, lead.id()))
                .hasSize(2)
                .allSatisfy(note -> assertThat(note.get("status")).isEqualTo("ENROLLED"));
        assertThatThrownBy(
                        () ->
                                service.reserveTrial(
                                        staff,
                                        lead.id(),
                                        new EngagementModels.TrialInput("lesson")))
                .hasMessageContaining("报名");
        verifyNoInteractions(client);
    }
}
