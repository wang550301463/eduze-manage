package com.eduze.manage.curriculum;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CurriculumSeedIT extends AbstractITContainerTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void stageCount_is5() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_curriculum_stage WHERE deleted_at=0", Integer.class);
        assertThat(count).isEqualTo(5);
    }

    @Test
    void dimensionCount_is22_and_breakdown() {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_curriculum_dimension WHERE deleted_at=0", Integer.class);
        assertThat(total).isEqualTo(22);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_curriculum_dimension WHERE kind='ELEMENT' AND deleted_at=0",
                        Integer.class))
                .isEqualTo(7);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_curriculum_dimension WHERE kind='PRINCIPLE' AND deleted_at=0",
                        Integer.class))
                .isEqualTo(7);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_curriculum_dimension WHERE kind='MOVEMENT' AND deleted_at=0",
                        Integer.class))
                .isEqualTo(8);
    }

    @Test
    void stageDimensionMappingExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_stage_dimension", Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(20);
    }

    @Test
    void demoCourses_5() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_course WHERE deleted_at=0 AND name LIKE '%·%'", Integer.class);
        assertThat(count).isEqualTo(5);
    }
}
