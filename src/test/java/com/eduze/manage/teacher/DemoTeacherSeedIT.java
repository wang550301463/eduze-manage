package com.eduze.manage.teacher;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DemoTeacherSeedIT extends AbstractITContainerTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void threeDemoTeachersSeeded() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_user WHERE username IN ('teacher_zhang','teacher_wang','teacher_li') AND deleted_at=0",
                Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void eachDemoTeacherHasTeacherRole() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_user_role ur "
                        + "JOIN t_user u ON u.id=ur.user_id "
                        + "WHERE u.username IN ('teacher_zhang','teacher_wang','teacher_li') AND ur.role_id=4",
                Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void availabilitiesSeeded() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_teacher_availability WHERE deleted_at=0", Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(6);
    }

    @Test
    void demoStudentsHaveMentorAndSubscription() {
        Integer studentCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_student WHERE deleted_at=0 AND enroll_no LIKE 'DEMO%' AND mentor_teacher_id IS NOT NULL",
                Integer.class);
        assertThat(studentCount).isEqualTo(5);
        Integer subCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson_subscription WHERE deleted_at=0 AND status=1 AND student_id BETWEEN 240001 AND 240005",
                Integer.class);
        assertThat(subCount).isEqualTo(5);
    }
}
