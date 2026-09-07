package com.eduze.manage.curriculum;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationV151IT extends AbstractITContainerTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allCurriculumTablesExist() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN "
                        + "('t_curriculum_stage','t_curriculum_dimension','t_stage_dimension','t_student_stage_assessment')",
                Integer.class);
        assertThat(count).isEqualTo(4);
    }

    @Test
    void dimensionKindEnumColumn() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name='t_curriculum_dimension' AND column_name='kind'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
