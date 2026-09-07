package com.eduze.manage.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.lesson.dto.BulkGenerateRequest;
import com.eduze.manage.lesson.dto.BulkGenerateResult;
import com.eduze.manage.lesson.service.ScheduleGenerator;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * ScheduleGenerator 核心算法集成测试。基于 V9.2.0 dev seed 数据。
 * Task4 起仅已绑定分组的时段参与生成。
 */
class ScheduleGeneratorIT extends AbstractITContainerTest {

    private static final AtomicLong BIND_ID = new AtomicLong(880001);

    @Autowired
    private ScheduleGenerator generator;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void bindSeedAvailabilities() {
        // 只绑定 seed 时段，避免其它 IT 残留的临时 availability 干扰断言
        for (long availId = 230001L; availId <= 230006L; availId++) {
            Integer bound = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM t_class_group WHERE teacher_availability_id = ? AND deleted_at = 0",
                    Integer.class,
                    availId);
            if (bound != null && bound > 0) {
                continue;
            }
            Map<String, Object> a = jdbc.queryForMap(
                    "SELECT teacher_id, branch_id, capacity FROM t_teacher_availability WHERE id = ?",
                    availId);
            long id = BIND_ID.getAndIncrement();
            jdbc.update(
                    """
                    INSERT INTO t_class_group
                      (id, tenant_id, branch_id, name, course_id, head_teacher_id, teacher_availability_id,
                       capacity, status, created_at, updated_at, deleted_at, version)
                    VALUES (?, 1, ?, ?, NULL, ?, ?, ?, 1, NOW(3), NOW(3), 0, 1)
                    """,
                    id,
                    a.get("branch_id"),
                    "seed-bind-" + availId,
                    a.get("teacher_id"),
                    availId,
                    a.get("capacity"));
        }
    }

    @Test
    @Transactional
    void generates_lessons_for_active_availabilities() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(2);
        req.setBranchId(1L);
        BulkGenerateResult result = generator.generate(req);
        assertThat(result.getGenerated()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Transactional
    void skips_unbound_availability() {
        long unboundId = 239999L;
        jdbc.update("DELETE FROM t_class_group WHERE teacher_availability_id = ?", unboundId);
        jdbc.update("DELETE FROM t_teacher_availability WHERE id = ?", unboundId);
        jdbc.update(
                """
                INSERT INTO t_teacher_availability
                  (id, tenant_id, branch_id, teacher_id, day_of_week, start_minute, end_minute,
                   capacity, valid_from, status, created_at, updated_at, deleted_at, version)
                VALUES (?, 1, 1, 1101, 1, 100, 160, 4, '2026-05-01', 1, NOW(3), NOW(3), 0, 1)
                """,
                unboundId);

        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setTeacherIds(List.of(1101L));
        BulkGenerateResult result = generator.generate(req);

        Integer unboundLessons = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson WHERE teacher_availability_id = ?",
                Integer.class,
                unboundId);
        assertThat(unboundLessons).isEqualTo(0);
        assertThat(result.getGenerated()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Transactional
    void idempotent_skips_existing_lessons() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setBranchId(1L);

        BulkGenerateResult first = generator.generate(req);
        BulkGenerateResult second = generator.generate(req);

        assertThat(first.getGenerated()).isGreaterThan(0);
        assertThat(second.getSkipped()).isGreaterThanOrEqualTo(first.getGenerated());
        assertThat(second.getGenerated()).isEqualTo(0);
    }

    @Test
    @Transactional
    void skips_holidays() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setBranchId(1L);
        req.setHolidays(List.of(LocalDate.of(2026, 6, 6)));

        BulkGenerateResult result = generator.generate(req);
        Integer saturdayLessons = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson WHERE DATE(start_at) = '2026-06-06'",
                Integer.class);
        assertThat(saturdayLessons).isEqualTo(0);
        assertThat(result.getGenerated()).isGreaterThan(0);
    }

    @Test
    @Transactional
    void respects_valid_from_to() {
        jdbc.update(
                "UPDATE t_teacher_availability SET valid_to = ? WHERE id = 230001",
                LocalDate.of(2026, 5, 15));

        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setTeacherIds(List.of(1101L));
        BulkGenerateResult result = generator.generate(req);

        Integer rowsFor230001 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson WHERE teacher_availability_id = 230001 AND DATE(start_at) >= '2026-06-01'",
                Integer.class);
        assertThat(rowsFor230001).isEqualTo(0);
        assertThat(result.getGenerated()).isGreaterThanOrEqualTo(1);

        jdbc.update("UPDATE t_teacher_availability SET valid_to = NULL WHERE id = 230001");
    }

    @Test
    @Transactional
    void filters_by_branch() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setBranchId(999L);
        BulkGenerateResult result = generator.generate(req);
        assertThat(result.getGenerated()).isEqualTo(0);
    }

    @Test
    @Transactional
    void filters_by_teacher_ids() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setTeacherIds(List.of(1101L));
        BulkGenerateResult result = generator.generate(req);
        // seed 周六/周日各 1；其它 IT 可能留下额外已绑定时段
        assertThat(result.getGenerated()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Transactional
    void skips_disabled_availability() {
        jdbc.update("UPDATE t_teacher_availability SET status = 0 WHERE id = 230001");

        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setTeacherIds(List.of(1101L));
        BulkGenerateResult result = generator.generate(req);
        Integer forDisabled = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson WHERE teacher_availability_id = 230001 AND DATE(start_at) >= '2026-06-01'",
                Integer.class);
        assertThat(forDisabled).isEqualTo(0);
        assertThat(result.getGenerated()).isGreaterThanOrEqualTo(1);

        jdbc.update("UPDATE t_teacher_availability SET status = 1 WHERE id = 230001");
    }

    @Test
    @Transactional
    void adds_subscribers_to_roster() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setTeacherIds(List.of(1101L));
        BulkGenerateResult result = generator.generate(req);
        assertThat(result.getRosterAdded()).isGreaterThanOrEqualTo(1);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson_student ls "
                        + "JOIN t_lesson l ON l.id = ls.lesson_id "
                        + "WHERE ls.student_id = 240001 AND ls.status = 'BOOKED' AND DATE(l.start_at) >= '2026-06-01'",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Transactional
    void max_8_weeks_bounded() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(8);
        req.setTeacherIds(List.of(1101L));
        BulkGenerateResult result = generator.generate(req);
        // seed 2 时段 × 8 周 = 16；允许其它已绑定时段导致更多
        assertThat(result.getGenerated()).isGreaterThanOrEqualTo(16);
    }

    @Test
    @Transactional
    void zero_active_availabilities_returns_zero() {
        jdbc.update("UPDATE t_teacher_availability SET status = 0");

        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        BulkGenerateResult result = generator.generate(req);
        assertThat(result.getGenerated()).isEqualTo(0);

        jdbc.update("UPDATE t_teacher_availability SET status = 1");
    }
}
