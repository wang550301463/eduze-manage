package com.eduze.manage.teacher;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationV150IT extends AbstractITContainerTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void teacherAvailability_tableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='t_teacher_availability'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void lessonSubscription_tableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='t_lesson_subscription'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void lessonStudent_tableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='t_lesson_student'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void studentMentorHistory_tableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='t_student_mentor_history'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void student_hasMentorTeacherIdColumn() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name='t_student' AND column_name='mentor_teacher_id'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void student_hasCurrentStageIdColumn() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name='t_student' AND column_name='current_stage_id'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void lesson_hasTeacherAvailabilityIdAndSource() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name='t_lesson' AND column_name IN ('teacher_availability_id','source')",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void classGroup_courseIdNullable() {
        String nullable = jdbc.queryForObject(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_name='t_class_group' AND column_name='course_id'",
                String.class);
        assertThat(nullable).isEqualTo("YES");
    }

    @Test
    void lesson_classGroupIdNullable() {
        String nullable = jdbc.queryForObject(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_name='t_lesson' AND column_name='class_group_id'",
                String.class);
        assertThat(nullable).isEqualTo("YES");
    }
}
