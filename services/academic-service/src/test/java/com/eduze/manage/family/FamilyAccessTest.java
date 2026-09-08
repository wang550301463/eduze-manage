package com.eduze.manage.family;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;

class FamilyAccessTest {
    private JdbcTemplate jdbc;
    private FamilyService families;
    private AcademicAccess access;
    private Outbox outbox;

    @BeforeEach
    void setup() {
        jdbc =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                "jdbc:h2:mem:"
                                        + UUID.randomUUID()
                                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                                "sa",
                                ""));
        jdbc.execute(
                "CREATE TABLE t_student(id BIGINT PRIMARY KEY,tenant_id BIGINT,branch_id BIGINT,name VARCHAR(64),mentor_teacher_id BIGINT,deleted_at BIGINT DEFAULT 0)");
        jdbc.execute(
                "CREATE TABLE family_invite(id VARCHAR(36) PRIMARY KEY,tenant_id BIGINT,student_id BIGINT,branch_id BIGINT,code_hash VARCHAR(64) UNIQUE,expires_at TIMESTAMP,claimed_by BIGINT,created_by BIGINT)");
        jdbc.execute(
                "CREATE TABLE family_binding(id VARCHAR(36) PRIMARY KEY,tenant_id BIGINT,student_id BIGINT,branch_id BIGINT,user_id BIGINT,status VARCHAR(16),invite_id VARCHAR(36) UNIQUE,approved_by BIGINT,approved_at TIMESTAMP,revoked_at TIMESTAMP,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(tenant_id,student_id,user_id))");
        jdbc.execute(
                "CREATE TABLE t_lesson(id BIGINT,tenant_id BIGINT,branch_id BIGINT,class_group_id BIGINT,teacher_id BIGINT,deleted_at BIGINT DEFAULT 0)");
        jdbc.execute(
                "CREATE TABLE t_lesson_student(tenant_id BIGINT,student_id BIGINT,lesson_id BIGINT,deleted_at BIGINT DEFAULT 0,status VARCHAR(16))");
        jdbc.update(
                "INSERT INTO t_student(id,tenant_id,branch_id,name,mentor_teacher_id) VALUES(10,1,2,'孩子',5),(11,1,3,'其他校区',6)");
        access = new AcademicAccess(jdbc);
        outbox = mock(Outbox.class);
        families = new FamilyService(jdbc, access, outbox);
    }

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void actor(String user, String... roles) {
        var req = new MockHttpServletRequest();
        req.setAttribute(
                Actors.ATTRIBUTE,
                new Actor(
                        user,
                        "1",
                        Set.of("2"),
                        Set.of(roles),
                        Set.of("guardian:write", "student:read")));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @Test
    void invitationNeedsStaffApprovalAndRevocationTakesEffectImmediately() {
        actor("1", "SUPER_ADMIN");
        var invite = families.invite("10");
        actor("20");
        var pending = families.claim(invite.code());
        assertEquals("PENDING", pending.status());
        assertTrue(families.children().isEmpty());
        assertThrows(PlatformException.class, () -> access.student("10"));
        assertEquals(pending.id(), families.claim(invite.code()).id());
        actor("1", "SUPER_ADMIN");
        families.approve(pending.id());
        actor("20");
        assertEquals("10", families.children().get(0).id());
        assertEquals("10", access.student("10").id());
        actor("21");
        assertThrows(PlatformException.class, () -> access.student("10"));
        assertThrows(PlatformException.class, () -> families.claim(invite.code()));
        actor("1", "SUPER_ADMIN");
        families.revoke(pending.id());
        actor("20");
        assertThrows(PlatformException.class, () -> access.student("10"));
        assertTrue(families.children().isEmpty());
        verify(outbox, times(2))
                .enqueue(
                        eq("notification"),
                        eq("academic.family-authorization-changed"),
                        eq("1"),
                        eq("2"),
                        eq(pending.id()),
                        any());
    }

    @Test
    void expiredInvitesAndStudentIdGuessingDoNotGrantAccess() {
        actor("1", "SUPER_ADMIN");
        var invite = families.invite("10");
        jdbc.update(
                "UPDATE family_invite SET expires_at=? WHERE id=?",
                Timestamp.from(Instant.now().minusSeconds(1)),
                invite.id());
        actor("20");
        assertThrows(PlatformException.class, () -> families.claim(invite.code()));
        assertThrows(PlatformException.class, () -> families.balance("10"));
    }

    @Test
    void teacherNeedsLiveTeachingRelationAndCorrectBranch() {
        actor("5", "TEACHER");
        assertEquals("10", access.student("10").id());
        assertThrows(PlatformException.class, () -> access.student("11"));
        actor("6", "TEACHER");
        assertThrows(PlatformException.class, () -> access.student("10"));
        jdbc.update("INSERT INTO t_lesson(id,tenant_id,branch_id,teacher_id) VALUES(100,1,2,6)");
        jdbc.update(
                "INSERT INTO t_lesson_student(tenant_id,student_id,lesson_id,status) VALUES(1,10,100,'BOOKED')");
        assertEquals("10", access.student("10").id());
    }
}
