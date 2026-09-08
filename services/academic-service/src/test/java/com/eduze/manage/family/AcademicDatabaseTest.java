package com.eduze.manage.family;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.manage.AcademicApplication;
import com.eduze.platform.runtime.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
@SpringBootTest(
        classes = AcademicApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "eduze.jwt.secret=test-jwt-secret-minimum-32-chars-long",
            "eduze.runtime.service-name=academic",
            "eduze.runtime.service-token=test-academic-service-token-123456",
            "spring.data.redis.password=test-password",
            "eduze.runtime.callers.portfolio=test-portfolio-service-token-123456",
            "eduze.runtime.callers.notification=test-notification-service-token-123456",
            "eduze.runtime.callers.engagement=test-engagement-service-token-123456",
            "eduze.runtime.callers.commerce=test-commerce-service-token-123456",
            "eduze.security.login-rate-limit-enabled=false"
        })
class AcademicDatabaseTest {
    @Container
    static org.testcontainers.containers.GenericContainer<?> redis =
            new org.testcontainers.containers.GenericContainer<>("redis:7.2-alpine")
                    .withExposedPorts(6379)
                    .withCommand("redis-server", "--requirepass", "test-password");

    @Container static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired com.eduze.manage.lesson.service.LessonStudentService lessonRosters;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestRestTemplate http;
    @MockitoBean InternalClient remote;

    @Test
    void standaloneSchemaStaffBridgeAndFamilyAccess() {
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('t_user','t_branch','t_curriculum_stage')",
                        Integer.class));
        jdbc.update(
                "INSERT INTO t_student(id,tenant_id,branch_id,enroll_no,name,mentor_teacher_id) VALUES(10,1,2,'S10','孩子',5)");
        jdbc.update(
                "INSERT INTO family_invite(id,tenant_id,student_id,branch_id,code_hash,expires_at,created_by) VALUES('invite',1,10,2,REPEAT('a',64),DATE_ADD(NOW(),INTERVAL 1 DAY),1)");
        jdbc.update(
                "INSERT INTO family_binding(id,tenant_id,student_id,branch_id,user_id,status,invite_id) VALUES('family',1,10,2,20,'ACTIVE','invite')");
        when(remote.post(
                        eq("identity"),
                        eq("/internal/identity/introspect"),
                        any(),
                        eq(Actor.class)))
                .thenReturn(new Actor("20", "1", Set.of(), Set.of(), Set.of()));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("verified-parent-token");
        var family =
                http.exchange(
                        "/api/v1/academic/family/children",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class);
        assertEquals(200, family.getStatusCode().value());
        assertEquals(1, ((List<?>) family.getBody().get("data")).size());
        var denied =
                http.exchange(
                        "/api/students", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(403, denied.getStatusCode().value());
        jdbc.update("UPDATE family_binding SET status='REVOKED' WHERE id='family'");
        var revoked =
                http.exchange(
                        "/api/v1/academic/family/children/10/balance",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);
        assertEquals(403, revoked.getStatusCode().value());
    }

    @Test
    void concurrentCheckInConsumesOnceAndVoidRestoresOriginalPackageOnce() throws Exception {
        jdbc.update(
                "INSERT INTO t_student(id,tenant_id,branch_id,enroll_no,name,mentor_teacher_id) VALUES(43,1,2,'S43','签到孩子',5),(44,1,2,'S44','未入班孩子',5)");
        jdbc.update(
                "INSERT INTO t_course(id,tenant_id,name,lesson_minutes) VALUES(140,1,'绘画课',90)");
        jdbc.update(
                "INSERT INTO t_class_group(id,tenant_id,branch_id,name,course_id) VALUES(141,1,2,'绘画班',140)");
        jdbc.update(
                "INSERT INTO t_lesson(id,tenant_id,branch_id,class_group_id,start_at,end_at,status,source) VALUES(142,1,2,141,NOW(),DATE_ADD(NOW(),INTERVAL 90 MINUTE),'SCHEDULED',2)");
        jdbc.update(
                "INSERT INTO t_lesson_student(id,tenant_id,branch_id,lesson_id,student_id,source,status) VALUES(143,1,2,142,43,'MANUAL','BOOKED')");
        jdbc.update(
                "INSERT INTO t_course_package(id,tenant_id,branch_id,student_id,total_lessons,remaining_lessons,course_id,expire_date) VALUES(144,1,2,43,5,5,140,DATE_ADD(CURDATE(),INTERVAL 30 DAY)),(145,1,2,43,5,5,999,DATE_ADD(CURDATE(),INTERVAL 1 DAY))");
        when(remote.post(
                        eq("identity"),
                        eq("/internal/identity/introspect"),
                        any(),
                        eq(Actor.class)))
                .thenReturn(
                        new Actor(
                                "1",
                                "1",
                                Set.of("2"),
                                Set.of("SUPER_ADMIN"),
                                Set.of("attendance:write", "student:hour_adjust", "student:read")));
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth("verified-admin-token");
        var request =
                new HttpEntity<>(
                        Map.of("lessonId", "142", "studentId", "43", "method", "manual"), h);
        var first =
                java.util.concurrent.CompletableFuture.supplyAsync(
                        () ->
                                http.postForEntity(
                                                "/api/attendance/check-in", request, String.class)
                                        .getStatusCode()
                                        .value());
        var second =
                java.util.concurrent.CompletableFuture.supplyAsync(
                        () ->
                                http.postForEntity(
                                                "/api/attendance/check-in", request, String.class)
                                        .getStatusCode()
                                        .value());
        var codes = new java.util.ArrayList<>(List.of(first.get(), second.get()));
        java.util.Collections.sort(codes);
        assertEquals(List.of(200, 409), codes);
        assertEquals(
                4,
                jdbc.queryForObject(
                        "SELECT remaining_lessons FROM t_course_package WHERE id=144",
                        Integer.class));
        assertEquals(
                5,
                jdbc.queryForObject(
                        "SELECT remaining_lessons FROM t_course_package WHERE id=145",
                        Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_student_lesson_hour_ledger WHERE student_id=43 AND event_type='ATTEND'",
                        Integer.class));
        assertEquals(
                403,
                http.postForEntity(
                                "/api/attendance/check-in",
                                new HttpEntity<>(
                                        Map.of(
                                                "lessonId",
                                                "142",
                                                "studentId",
                                                "44",
                                                "method",
                                                "manual"),
                                        h),
                                String.class)
                        .getStatusCode()
                        .value());
        String ledger =
                jdbc.queryForObject(
                        "SELECT id FROM t_student_lesson_hour_ledger WHERE student_id=43 AND event_type='ATTEND'",
                        String.class);
        var reversal =
                new HttpEntity<>(
                        Map.of(
                                "eventType",
                                "VOID",
                                "minutesDelta",
                                90,
                                "relatedLedgerId",
                                ledger,
                                "note",
                                "纠正错误签到"),
                        h);
        assertEquals(
                200,
                http.postForEntity(
                                "/api/students/43/lesson-hour-ledger/adjust",
                                reversal,
                                String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                409,
                http.postForEntity(
                                "/api/students/43/lesson-hour-ledger/adjust",
                                reversal,
                                String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                5,
                jdbc.queryForObject(
                        "SELECT remaining_lessons FROM t_course_package WHERE id=144",
                        Integer.class));
    }

    @Test
    void lessonSpecificLeaveDoesNotAffectSameDayAndCannotOverwriteCheckIn() {
        jdbc.update(
                "INSERT INTO t_student(id,tenant_id,branch_id,enroll_no,name) VALUES(603,1,2,'S603','请假学员')");
        jdbc.update(
                "INSERT INTO t_lesson(id,tenant_id,branch_id,start_at,end_at,status,source) VALUES(604,1,2,NOW(),DATE_ADD(NOW(),INTERVAL 1 HOUR),'SCHEDULED',2),(605,1,2,NOW(),DATE_ADD(NOW(),INTERVAL 1 HOUR),'SCHEDULED',2)");
        jdbc.update(
                "INSERT INTO t_lesson_student(id,tenant_id,branch_id,lesson_id,student_id,source,status) VALUES(606,1,2,604,603,'MANUAL','BOOKED'),(607,1,2,605,603,'MANUAL','BOOKED')");
        jdbc.update(
                "INSERT INTO t_course_package(id,tenant_id,branch_id,student_id,total_lessons,remaining_lessons) VALUES(608,1,2,603,2,2)");
        jdbc.update(
                "INSERT INTO t_leave_request(id,tenant_id,branch_id,student_id,lesson_id,leave_start_date,leave_end_date,status) VALUES(609,1,2,603,604,CURDATE(),CURDATE(),1)");
        when(remote.post(
                        eq("identity"),
                        eq("/internal/identity/introspect"),
                        any(),
                        eq(Actor.class)))
                .thenReturn(
                        new Actor(
                                "1",
                                "1",
                                Set.of("2"),
                                Set.of("SUPER_ADMIN"),
                                Set.of("leave:approve", "attendance:write")));
        var h = new HttpHeaders();
        h.setBearerAuth("staff");
        assertEquals(
                200,
                http.postForEntity("/api/leaves/609/approve", new HttpEntity<>(h), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_attendance WHERE student_id=603 AND lesson_id=604 AND status=5",
                        Integer.class));
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_attendance WHERE student_id=603 AND lesson_id=605",
                        Integer.class));
        assertEquals(
                200,
                http.postForEntity(
                                "/api/attendance/check-in",
                                new HttpEntity<>(
                                        Map.of(
                                                "lessonId",
                                                "605",
                                                "studentId",
                                                "603",
                                                "method",
                                                "manual"),
                                        h),
                                String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT remaining_lessons FROM t_course_package WHERE id=608",
                        Integer.class));
        jdbc.update(
                "INSERT INTO t_leave_request(id,tenant_id,branch_id,student_id,lesson_id,leave_start_date,leave_end_date,status) VALUES(610,1,2,603,605,CURDATE(),CURDATE(),1),(611,1,2,603,NULL,CURDATE(),CURDATE(),1)");
        assertEquals(
                409,
                http.postForEntity("/api/leaves/610/approve", new HttpEntity<>(h), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                409,
                http.postForEntity("/api/leaves/611/approve", new HttpEntity<>(h), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                2,
                jdbc.queryForObject(
                        "SELECT status FROM t_attendance WHERE student_id=603 AND lesson_id=605",
                        Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT status FROM t_leave_request WHERE id=610", Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT remaining_lessons FROM t_course_package WHERE id=608",
                        Integer.class));
        jdbc.update(
                "INSERT INTO t_student(id,tenant_id,branch_id,enroll_no,name) VALUES(612,1,2,'S612','日期请假学员')");
        jdbc.update(
                "INSERT INTO t_lesson_student(id,tenant_id,branch_id,lesson_id,student_id,source,status) VALUES(613,1,2,604,612,'MANUAL','BOOKED'),(614,1,2,605,612,'MANUAL','BOOKED')");
        jdbc.update(
                "INSERT INTO t_leave_request(id,tenant_id,branch_id,student_id,lesson_id,leave_start_date,leave_end_date,status) VALUES(615,1,2,612,NULL,CURDATE(),CURDATE(),1)");
        assertEquals(
                200,
                http.postForEntity("/api/leaves/615/approve", new HttpEntity<>(h), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                2,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_attendance WHERE student_id=612 AND status=5",
                        Integer.class));
    }

    @Test
    void rosterChangesAdvanceEventsAndNotificationAuthorityImmediately() {
        jdbc.update(
                "INSERT INTO t_student(id,tenant_id,branch_id,enroll_no,name) VALUES(703,1,2,'S703','名单学员')");
        jdbc.update(
                "INSERT INTO t_class_group(id,tenant_id,branch_id,name,capacity) VALUES(704,1,2,'通知班',3)");
        jdbc.update(
                "INSERT INTO t_lesson(id,tenant_id,branch_id,class_group_id,start_at,end_at,status,source) VALUES(705,1,2,704,DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 25 HOUR),'SCHEDULED',2)");
        when(remote.post(
                        eq("identity"),
                        eq("/internal/identity/introspect"),
                        any(),
                        eq(Actor.class)))
                .thenReturn(
                        new Actor(
                                "1",
                                "1",
                                Set.of("2"),
                                Set.of("SUPER_ADMIN"),
                                Set.of("lesson:write")));
        var h = new HttpHeaders();
        h.setBearerAuth("staff");
        var add = new HttpEntity<>(Map.of("studentId", "703", "source", "MANUAL"), h);
        assertEquals(
                200,
                http.postForEntity("/api/lessons/705/students", add, String.class)
                        .getStatusCode()
                        .value());
        String access = "/internal/academic/lessons/705/students/703/notification-access";
        var before =
                http.exchange(
                        access,
                        HttpMethod.GET,
                        new HttpEntity<>(serviceHeaders("notification")),
                        Map.class);
        assertEquals(true, before.getBody().get("allowed"));
        int first = ((Number) before.getBody().get("revision")).intValue();
        assertEquals(
                200,
                http.exchange(
                                "/api/lessons/705/students/703",
                                HttpMethod.DELETE,
                                new HttpEntity<>(h),
                                String.class)
                        .getStatusCode()
                        .value());
        var removed =
                http.exchange(
                        access,
                        HttpMethod.GET,
                        new HttpEntity<>(serviceHeaders("notification")),
                        Map.class);
        assertEquals(false, removed.getBody().get("allowed"));
        assertTrue(((Number) removed.getBody().get("revision")).intValue() > first);
        assertEquals(
                200,
                http.postForEntity("/api/lessons/705/students", add, String.class)
                        .getStatusCode()
                        .value());
        var again =
                http.exchange(
                        access,
                        HttpMethod.GET,
                        new HttpEntity<>(serviceHeaders("notification")),
                        Map.class);
        assertEquals(true, again.getBody().get("allowed"));
        assertTrue(
                ((Number) again.getBody().get("revision")).intValue()
                        > ((Number) removed.getBody().get("revision")).intValue());
        var envelopes =
                jdbc.queryForList(
                        "SELECT envelope FROM platform_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(envelope,'$.type'))='academic.lesson-changed' AND JSON_UNQUOTE(JSON_EXTRACT(envelope,'$.aggregateId'))='705' ORDER BY created_at",
                        String.class);
        assertEquals(3, envelopes.size());
        var eventRevisions =
                jdbc.queryForList(
                        "SELECT CAST(JSON_UNQUOTE(JSON_EXTRACT(envelope,'$.payload.revision')) AS SIGNED) FROM platform_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(envelope,'$.type'))='academic.lesson-changed' AND JSON_UNQUOTE(JSON_EXTRACT(envelope,'$.aggregateId'))='705' ORDER BY created_at",
                        Integer.class);
        assertEquals(
                List.of(
                        first,
                        ((Number) removed.getBody().get("revision")).intValue(),
                        ((Number) again.getBody().get("revision")).intValue()),
                eventRevisions);
        assertEquals(List.of(2, 3, 4), eventRevisions);
        assertEquals(
                eventRevisions.get(2),
                jdbc.queryForObject("SELECT version FROM t_lesson WHERE id=705", Integer.class));
        assertTrue(envelopes.get(0).contains("703"));
        assertTrue(envelopes.get(1).contains("\"studentIds\":[]"));
    }

    @Test
    void unsubscribingCancelsFutureSubscriptionRosterButPreservesManualBookings() {
        jdbc.update(
                "INSERT INTO t_lesson(id,tenant_id,branch_id,teacher_availability_id,start_at,end_at,status,source) VALUES(801,1,2,803,DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 25 HOUR),'SCHEDULED',1),(802,1,2,803,DATE_ADD(NOW(),INTERVAL 2 DAY),DATE_ADD(NOW(),INTERVAL 49 HOUR),'SCHEDULED',1)");
        jdbc.update(
                "INSERT INTO t_lesson_student(id,tenant_id,branch_id,lesson_id,student_id,source,status) VALUES(804,1,2,801,805,'SUBSCRIPTION','BOOKED'),(806,1,2,802,805,'MANUAL','BOOKED')");
        com.eduze.manage.tenant.TenantContext.setTenantId(1L);
        try {
            lessonRosters.cancelFuture(805L, 803L);
        } finally {
            com.eduze.manage.tenant.TenantContext.clear();
        }
        assertEquals(
                "CANCELLED",
                jdbc.queryForObject(
                        "SELECT status FROM t_lesson_student WHERE id=804", String.class));
        assertEquals(
                "BOOKED",
                jdbc.queryForObject(
                        "SELECT status FROM t_lesson_student WHERE id=806", String.class));
        assertEquals(
                2, jdbc.queryForObject("SELECT version FROM t_lesson WHERE id=801", Integer.class));
        assertEquals(
                1, jdbc.queryForObject("SELECT version FROM t_lesson WHERE id=802", Integer.class));
    }

    private HttpHeaders serviceHeaders(String name) {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Service-Name", name);
        h.set("X-Service-Token", "test-" + name + "-service-token-123456");
        return h;
    }

    @Test
    void trialBookingSharesLockedCapacityAndCancellationReleasesIt() throws Exception {
        jdbc.update(
                "INSERT INTO t_class_group(id,tenant_id,branch_id,name,capacity) VALUES(201,1,2,'试听班',1)");
        jdbc.update(
                "INSERT INTO t_lesson(id,tenant_id,branch_id,class_group_id,start_at,end_at,status,source) VALUES(202,1,2,201,DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 25 HOUR),'SCHEDULED',2)");
        var headers = serviceHeaders("engagement");
        var a =
                Map.of(
                        "reservationId",
                        "trial-a",
                        "tenantId",
                        "1",
                        "branchId",
                        "2",
                        "sessionId",
                        "202",
                        "studentName",
                        "试听甲",
                        "contactPhone",
                        "13800000000");
        var b = new HashMap<>(a);
        b.put("reservationId", "trial-b");
        var first =
                java.util.concurrent.CompletableFuture.supplyAsync(
                        () ->
                                http.postForEntity(
                                        "/internal/academic/trial-reservations",
                                        new HttpEntity<>(a, headers),
                                        String.class));
        var second =
                java.util.concurrent.CompletableFuture.supplyAsync(
                        () ->
                                http.postForEntity(
                                        "/internal/academic/trial-reservations",
                                        new HttpEntity<>(b, headers),
                                        String.class));
        var one = first.get();
        var two = second.get();
        var codes =
                new ArrayList<>(List.of(one.getStatusCode().value(), two.getStatusCode().value()));
        Collections.sort(codes);
        assertEquals(List.of(200, 409), codes);
        var winner = one.getStatusCode().value() == 200 ? a : b;
        String id = winner.get("reservationId");
        assertEquals(
                200,
                http.postForEntity(
                                "/internal/academic/trial-reservations",
                                new HttpEntity<>(winner, headers),
                                String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_lesson_student WHERE lesson_id=202 AND status='BOOKED'",
                        Integer.class));
        jdbc.update(
                "INSERT INTO t_student(id,tenant_id,branch_id,enroll_no,name) VALUES(209,1,2,'S209','正式学员')");
        when(remote.post(
                        eq("identity"),
                        eq("/internal/identity/introspect"),
                        any(),
                        eq(Actor.class)))
                .thenReturn(
                        new Actor(
                                "1",
                                "1",
                                Set.of("2"),
                                Set.of("SUPER_ADMIN"),
                                Set.of("lesson:write")));
        var staff = new HttpHeaders();
        staff.setBearerAuth("verified-admin");
        assertEquals(
                409,
                http.postForEntity(
                                "/api/lessons/202/students",
                                new HttpEntity<>(
                                        Map.of("studentId", "209", "source", "MANUAL"), staff),
                                String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                200,
                http.postForEntity(
                                "/internal/academic/trial-reservations/" + id + "/cancel",
                                new HttpEntity<>(headers),
                                String.class)
                        .getStatusCode()
                        .value());
        var next = new HashMap<>(a);
        next.put("reservationId", "trial-c");
        assertEquals(
                200,
                http.postForEntity(
                                "/internal/academic/trial-reservations",
                                new HttpEntity<>(next, headers),
                                String.class)
                        .getStatusCode()
                        .value());
        next.put("tenantId", "999");
        assertEquals(
                403,
                http.postForEntity(
                                "/internal/academic/trial-reservations",
                                new HttpEntity<>(next, headers),
                                String.class)
                        .getStatusCode()
                        .value());
    }

    @Test
    void trialEnrollmentRequiresStaffAndCreatesRealMembershipBeforeEvent() {
        jdbc.update(
                "INSERT INTO t_teacher_availability(id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status) VALUES(501,1,2,5,1,540,600,3,CURDATE(),1)");
        jdbc.update(
                "INSERT INTO t_class_group(id,tenant_id,branch_id,name,capacity,teacher_availability_id,status) VALUES(502,1,2,'转正班',3,501,1)");
        jdbc.update(
                "INSERT INTO t_lesson(id,tenant_id,branch_id,class_group_id,teacher_id,teacher_availability_id,start_at,end_at,status,source) VALUES(503,1,2,502,5,501,DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 25 HOUR),'SCHEDULED',2)");
        var headers = serviceHeaders("engagement");
        var reservation =
                Map.of(
                        "reservationId",
                        "trial-enrollment",
                        "tenantId",
                        "1",
                        "branchId",
                        "2",
                        "sessionId",
                        "503",
                        "studentName",
                        "报名学员",
                        "contactPhone",
                        "13800000000");
        var reserved =
                http.postForEntity(
                        "/internal/academic/trial-reservations",
                        new HttpEntity<>(reservation, headers),
                        Map.class);
        assertEquals(200, reserved.getStatusCode().value());
        String student = reserved.getBody().get("studentId").toString();
        var enrollment =
                Map.of(
                        "enquiryId",
                        "enquiry-501",
                        "enrollmentId",
                        "enrollment-501",
                        "classGroupId",
                        "502");
        String path = "/internal/academic/trial-reservations/trial-enrollment/enroll";
        assertEquals(
                401,
                http.postForEntity(path, new HttpEntity<>(enrollment, headers), String.class)
                        .getStatusCode()
                        .value());
        headers.setBearerAuth("verified-staff");
        when(remote.post(
                        eq("identity"),
                        eq("/internal/identity/introspect"),
                        any(),
                        eq(Actor.class)))
                .thenReturn(
                        new Actor(
                                "1",
                                "1",
                                Set.of("9"),
                                Set.of("FRONT_DESK"),
                                Set.of("classgroup:assign")));
        assertEquals(
                403,
                http.postForEntity(path, new HttpEntity<>(enrollment, headers), String.class)
                        .getStatusCode()
                        .value());
        when(remote.post(
                        eq("identity"),
                        eq("/internal/identity/introspect"),
                        any(),
                        eq(Actor.class)))
                .thenReturn(
                        new Actor(
                                "1",
                                "1",
                                Set.of("2"),
                                Set.of("FRONT_DESK"),
                                Set.of("classgroup:assign")));
        assertEquals(
                200,
                http.postForEntity(path, new HttpEntity<>(enrollment, headers), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                200,
                http.postForEntity(path, new HttpEntity<>(enrollment, headers), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_student_class_group WHERE student_id=? AND class_group_id=502 AND left_at IS NULL",
                        Integer.class,
                        student));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_lesson_subscription WHERE student_id=? AND teacher_availability_id=501 AND status=1",
                        Integer.class,
                        student));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM platform_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(envelope,'$.type'))='academic.enrollment-confirmed' AND JSON_UNQUOTE(JSON_EXTRACT(envelope,'$.aggregateId'))='enrollment-501'",
                        Integer.class));
        assertEquals(
                409,
                http.postForEntity(
                                "/internal/academic/trial-reservations/trial-enrollment/cancel",
                                new HttpEntity<>(headers),
                                String.class)
                        .getStatusCode()
                        .value());
    }

    @Test
    void paidEventIdempotenceFrozenRightsAndRefundTransitions() throws Exception {
        jdbc.update(
                "INSERT INTO t_student(id,tenant_id,branch_id,enroll_no,name) VALUES(303,1,2,'S303','商城学员')");
        jdbc.update("INSERT INTO t_course(id,tenant_id,name) VALUES(304,1,'商城课程')");
        var headers = serviceHeaders("commerce");
        var payload =
                Map.of(
                        "orderId",
                        "order-303",
                        "studentId",
                        "303",
                        "courseId",
                        "304",
                        "quantity",
                        2,
                        "lessonUnits",
                        10,
                        "totalMinor",
                        10000);
        var event =
                new EventEnvelope(
                        "paid-303",
                        "commerce.order-paid",
                        1,
                        "1",
                        "2",
                        "order-303",
                        java.time.Instant.now(),
                        "test",
                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(payload));
        assertEquals(
                200,
                http.postForEntity(
                                "/internal/events", new HttpEntity<>(event, headers), String.class)
                        .getStatusCode()
                        .value());
        var replay =
                new EventEnvelope(
                        "paid-303-replayed",
                        "commerce.order-paid",
                        1,
                        "1",
                        "2",
                        "order-303",
                        java.time.Instant.now(),
                        "test",
                        event.payload());
        assertEquals(
                200,
                http.postForEntity(
                                "/internal/events", new HttpEntity<>(replay, headers), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_course_package WHERE source_order_id='order-303'",
                        Integer.class));
        assertEquals(
                10,
                jdbc.queryForObject(
                        "SELECT remaining_lessons FROM t_course_package WHERE source_order_id='order-303'",
                        Integer.class));
        var freeze = new HashMap<String, Object>();
        freeze.put("refundId", "refund-303");
        freeze.put("orderId", "order-303");
        freeze.put("tenantId", "1");
        freeze.put("branchId", "2");
        freeze.put("studentId", "303");
        freeze.put("lessonUnits", 7);
        String base = "/internal/academic/commerce/refunds/";
        assertEquals(
                200,
                http.postForEntity(base + "freeze", new HttpEntity<>(freeze, headers), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                200,
                http.postForEntity(base + "freeze", new HttpEntity<>(freeze, headers), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                7,
                jdbc.queryForObject(
                        "SELECT frozen_lessons FROM t_course_package WHERE source_order_id='order-303'",
                        Integer.class));
        var extra = new HashMap<>(freeze);
        extra.put("refundId", "refund-extra");
        extra.put("lessonUnits", 4);
        assertEquals(
                409,
                http.postForEntity(base + "freeze", new HttpEntity<>(extra, headers), String.class)
                        .getStatusCode()
                        .value());
        extra.put("orderId", "unrelated-order");
        assertEquals(
                404,
                http.postForEntity(base + "freeze", new HttpEntity<>(extra, headers), String.class)
                        .getStatusCode()
                        .value());
        extra.put("orderId", "order-303");
        extra.put("studentId", "999");
        assertEquals(
                403,
                http.postForEntity(base + "freeze", new HttpEntity<>(extra, headers), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                200,
                http.postForEntity(
                                base + "complete", new HttpEntity<>(freeze, headers), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                200,
                http.postForEntity(
                                base + "complete", new HttpEntity<>(freeze, headers), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                409,
                http.postForEntity(
                                base + "release", new HttpEntity<>(freeze, headers), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                3,
                jdbc.queryForObject(
                        "SELECT remaining_lessons FROM t_course_package WHERE source_order_id='order-303'",
                        Integer.class));
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT frozen_lessons FROM t_course_package WHERE source_order_id='order-303'",
                        Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_student_lesson_hour_ledger WHERE external_reference='REFUND:refund-303'",
                        Integer.class));
        var released = new HashMap<>(freeze);
        released.put("refundId", "refund-release");
        released.put("lessonUnits", 2);
        assertEquals(
                200,
                http.postForEntity(
                                base + "freeze", new HttpEntity<>(released, headers), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                200,
                http.postForEntity(
                                base + "release", new HttpEntity<>(released, headers), String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                200,
                http.postForEntity(
                                base + "release", new HttpEntity<>(released, headers), String.class)
                        .getStatusCode()
                        .value());
        var replayReleased =
                http.postForEntity(base + "freeze", new HttpEntity<>(released, headers), Map.class);
        assertEquals("RELEASED", replayReleased.getBody().get("status"));
        assertEquals(
                409,
                http.postForEntity(
                                base + "complete",
                                new HttpEntity<>(released, headers),
                                String.class)
                        .getStatusCode()
                        .value());
        assertEquals(
                3,
                jdbc.queryForObject(
                        "SELECT remaining_lessons-frozen_lessons FROM t_course_package WHERE source_order_id='order-303'",
                        Integer.class));
    }

    @Test
    void refundAndAttendanceRaceCannotSpendSameRights() throws Exception {
        jdbc.update(
                "INSERT INTO t_student(id,tenant_id,branch_id,enroll_no,name) VALUES(403,1,2,'S403','竞争学员')");
        jdbc.update("INSERT INTO t_course(id,tenant_id,name) VALUES(404,1,'竞争课程')");
        jdbc.update(
                "INSERT INTO t_class_group(id,tenant_id,branch_id,name,course_id) VALUES(405,1,2,'竞争班',404)");
        jdbc.update(
                "INSERT INTO t_lesson(id,tenant_id,branch_id,class_group_id,start_at,end_at,status,source) VALUES(406,1,2,405,NOW(),DATE_ADD(NOW(),INTERVAL 1 HOUR),'SCHEDULED',2)");
        jdbc.update(
                "INSERT INTO t_lesson_student(id,tenant_id,branch_id,lesson_id,student_id,source,status) VALUES(407,1,2,406,403,'MANUAL','BOOKED')");
        jdbc.update(
                "INSERT INTO t_course_package(id,tenant_id,branch_id,student_id,course_id,total_lessons,remaining_lessons,source_order_id) VALUES(408,1,2,403,404,1,1,'order-403')");
        when(remote.post(
                        eq("identity"),
                        eq("/internal/identity/introspect"),
                        any(),
                        eq(Actor.class)))
                .thenReturn(
                        new Actor(
                                "1",
                                "1",
                                Set.of("2"),
                                Set.of("SUPER_ADMIN"),
                                Set.of("attendance:write")));
        var staff = new HttpHeaders();
        staff.setBearerAuth("verified-admin");
        var first =
                java.util.concurrent.CompletableFuture.supplyAsync(
                        () ->
                                http.postForEntity(
                                                "/api/attendance/check-in",
                                                new HttpEntity<>(
                                                        Map.of(
                                                                "lessonId",
                                                                "406",
                                                                "studentId",
                                                                "403",
                                                                "method",
                                                                "manual"),
                                                        staff),
                                                String.class)
                                        .getStatusCode()
                                        .value());
        var refund =
                Map.of(
                        "refundId",
                        "refund-403",
                        "orderId",
                        "order-403",
                        "tenantId",
                        "1",
                        "branchId",
                        "2",
                        "studentId",
                        "403",
                        "lessonUnits",
                        1);
        var second =
                java.util.concurrent.CompletableFuture.supplyAsync(
                        () ->
                                http.postForEntity(
                                                "/internal/academic/commerce/refunds/freeze",
                                                new HttpEntity<>(
                                                        refund, serviceHeaders("commerce")),
                                                String.class)
                                        .getStatusCode()
                                        .value());
        var codes = new ArrayList<>(List.of(first.get(), second.get()));
        Collections.sort(codes);
        assertEquals(List.of(200, 409), codes);
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT remaining_lessons-frozen_lessons FROM t_course_package WHERE id=408",
                        Integer.class));
    }
}
