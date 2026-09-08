package com.eduze.platform.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

class NotificationEventsTest {
    private JdbcTemplate jdbc;
    private Inbox inbox;
    private InternalClient client;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setup() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:notification-events;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("DROP ALL OBJECTS");
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/runtime/V0__runtime.sql"),
                        new ClassPathResource("db/migration/V1__notification.sql"))
                .execute(ds);
        client = mock(InternalClient.class);
        when(client.get(eq("academic"), anyString(), eq(JsonNode.class)))
                .thenReturn(
                        json.valueToTree(
                                Map.of(
                                        "tenantId",
                                        "1",
                                        "branchId",
                                        "2",
                                        "recipients",
                                        List.of(Map.of("userId", "parent")))));
        inbox =
                new Inbox(
                        jdbc,
                        new TransactionTemplate(new DataSourceTransactionManager(ds)),
                        List.of(new NotificationEvents(jdbc, client)));
    }

    @Test
    void reschedulingCancelsOldReminderAndRejectsStaleReplay() {
        var newer =
                event(
                        "new",
                        "academic.lesson-changed",
                        "lesson",
                        Map.of(
                                "revision",
                                2,
                                "status",
                                "SCHEDULED",
                                "startTime",
                                Instant.now().plusSeconds(172800).toString(),
                                "studentIds",
                                List.of("student")));
        assertTrue(inbox.receive(newer));
        assertFalse(inbox.receive(newer));
        inbox.receive(
                event(
                        "old",
                        "academic.lesson-changed",
                        "lesson",
                        Map.of(
                                "revision",
                                1,
                                "status",
                                "SCHEDULED",
                                "startTime",
                                Instant.now().plusSeconds(100).toString(),
                                "studentIds",
                                List.of("student"))));
        assertEquals(1, count("business_type='LESSON_REMINDER' AND visible=1"));
        inbox.receive(
                event(
                        "cancel",
                        "academic.lesson-changed",
                        "lesson",
                        Map.of(
                                "revision",
                                3,
                                "status",
                                "CANCELLED",
                                "studentIds",
                                List.of("student"))));
        assertEquals(0, count("business_type='LESSON_REMINDER' AND visible=1"));
        assertEquals(1, count("business_type='LESSON_CHANGED' AND visible=1"));
    }

    @Test
    void acceptsMinutePrecisionOffsetProducedByAcademicService() {
        String start =
                java.time.LocalDate.now()
                        .plusDays(3)
                        .atTime(9, 0)
                        .atOffset(java.time.ZoneOffset.ofHours(8))
                        .toString();
        assertTrue(
                inbox.receive(
                        event(
                                "minute-offset",
                                "academic.lesson-changed",
                                "lesson",
                                Map.of(
                                        "revision",
                                        1,
                                        "status",
                                        "SCHEDULED",
                                        "startTime",
                                        start,
                                        "studentIds",
                                        List.of("student")))));
        assertEquals(1, count("business_type='LESSON_REMINDER' AND visible=1"));
        Instant expected = java.time.OffsetDateTime.parse(start).toInstant().minusSeconds(86400);
        assertEquals(
                expected,
                jdbc.queryForObject(
                                "SELECT next_at FROM notification_message WHERE business_type='LESSON_REMINDER'",
                                java.sql.Timestamp.class)
                        .toInstant());
    }

    @Test
    void revokedFamilyIsRecheckedBeforeDeliveryEvenBeforeRevokeEventArrives() {
        inbox.receive(
                event(
                        "publication",
                        "portfolio.published",
                        "record",
                        Map.of("version", 1, "studentId", "student")));
        jdbc.update(
                "INSERT INTO notification_subscription VALUES('1','parent','PORTFOLIO_PUBLISHED',1)");
        when(client.get(eq("academic"), anyString(), eq(JsonNode.class)))
                .thenReturn(
                        json.valueToTree(
                                Map.of("tenantId", "1", "branchId", "2", "recipients", List.of())));
        var sender = mock(WechatSender.class);
        new NotificationDispatcher(jdbc, client, sender).dispatch();
        verifyNoInteractions(sender);
        assertEquals(
                1,
                count(
                        "status='SKIPPED' AND last_error='AUTHORIZATION_REVOKED' AND read_at IS NULL AND confirmed_at IS NULL"));
    }

    @Test
    void providerUnknownOutcomeIsNotAutomaticallyReplayed() {
        inbox.receive(
                event(
                        "publication",
                        "portfolio.published",
                        "record",
                        Map.of("version", 1, "studentId", "student")));
        jdbc.update(
                "INSERT INTO notification_subscription VALUES('1','parent','PORTFOLIO_PUBLISHED',1)");
        when(client.post(eq("identity"), anyString(), any(), eq(JsonNode.class)))
                .thenReturn(
                        json.valueToTree(List.of(Map.of("userId", "parent", "openId", "openid"))));
        var sender = mock(WechatSender.class);
        when(sender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenThrow(new IllegalStateException("unknown"));
        var worker = new NotificationDispatcher(jdbc, client, sender);
        worker.dispatch();
        worker.dispatch();
        verify(sender, times(1)).send(anyString(), anyString(), anyString(), anyMap());
        assertEquals(
                1, count("status='FAILED' AND last_error='DELIVERY_UNKNOWN' AND read_at IS NULL"));
    }

    @Test
    void delayedRevokeCannotHideMessagesCreatedAfterLiveReauthorization() {
        inbox.receive(
                event(
                        "publication",
                        "portfolio.published",
                        "record",
                        Map.of("version", 1, "studentId", "student")));
        // Academic has already accepted a new invitation; the older revocation arrives late.
        inbox.receive(
                event(
                        "old-revoke",
                        "academic.family-authorization-changed",
                        "old-binding",
                        Map.of("status", "REVOKED", "studentId", "student", "userId", "parent")));
        assertEquals(1, count("visible=1 AND status='PENDING'"));
        when(client.get(eq("academic"), anyString(), eq(JsonNode.class)))
                .thenReturn(json.valueToTree(Map.of("tenantId", "1", "recipients", List.of())));
        inbox.receive(
                event(
                        "current-revoke",
                        "academic.family-authorization-changed",
                        "new-binding",
                        Map.of("status", "REVOKED", "studentId", "student", "userId", "parent")));
        assertEquals(0, count("visible=1"));
    }

    @Test
    void removedRosterStudentDoesNotReceiveQueuedReminder() {
        inbox.receive(
                event(
                        "lesson-created",
                        "academic.lesson-changed",
                        "lesson",
                        Map.of(
                                "revision",
                                1,
                                "status",
                                "SCHEDULED",
                                "startTime",
                                Instant.now().plusSeconds(600).toString(),
                                "studentIds",
                                List.of("student"))));
        jdbc.update(
                "INSERT INTO notification_subscription VALUES('1','parent','LESSON_REMINDER',1)");
        when(client.get(eq("academic"), contains("/notification-access"), eq(JsonNode.class)))
                .thenReturn(
                        json.valueToTree(
                                Map.of(
                                        "allowed",
                                        false,
                                        "revision",
                                        2,
                                        "startAt",
                                        Instant.now().plusSeconds(600).toString())));
        var sender = mock(WechatSender.class);
        new NotificationDispatcher(jdbc, client, sender).dispatch();
        verifyNoInteractions(sender);
        assertEquals(
                1,
                count(
                        "business_type='LESSON_REMINDER' AND status='SKIPPED' AND last_error='STALE_SCHEDULE'"));
    }

    private int count(String condition) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_message WHERE " + condition, Integer.class);
    }

    private EventEnvelope event(
            String id, String type, String aggregate, Map<String, Object> payload) {
        return new EventEnvelope(
                id,
                type,
                1,
                "1",
                "2",
                aggregate,
                Instant.now(),
                "trace",
                json.valueToTree(payload));
    }
}
