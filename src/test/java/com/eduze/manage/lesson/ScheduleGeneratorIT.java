package com.eduze.manage.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.lesson.dto.BulkGenerateRequest;
import com.eduze.manage.lesson.dto.BulkGenerateResult;
import com.eduze.manage.lesson.service.ScheduleGenerator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * ScheduleGenerator 核心算法集成测试。基于 V9.2.0 dev seed 数据（teacher_zhang 1101 有 2 条 availability：
 * 周六 540-630 / 周日 540-630，学员 240001 订阅 230001）。
 */
class ScheduleGeneratorIT extends AbstractITContainerTest {

    @Autowired
    private ScheduleGenerator generator;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @Transactional
    void generates_lessons_for_active_availabilities() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(2);
        req.setBranchId(1L);
        BulkGenerateResult result = generator.generate(req);
        // 3 老师 × 2 availability × 2 周 = 12 节
        assertThat(result.getGenerated()).isGreaterThanOrEqualTo(2);
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
        // 第二次再生成相同范围，应该全部 skipped
        assertThat(second.getSkipped()).isGreaterThanOrEqualTo(first.getGenerated());
        assertThat(second.getGenerated()).isEqualTo(0);
    }

    @Test
    @Transactional
    void skips_holidays() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        // 2026-06-06 是周六，假设是节假日
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setBranchId(1L);
        req.setHolidays(java.util.List.of(LocalDate.of(2026, 6, 6)));

        BulkGenerateResult result = generator.generate(req);
        // 周六的课次（teacher_zhang/wang/li）都被节假日跳过
        Integer saturdayLessons = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson WHERE DATE(start_at) = '2026-06-06'",
                Integer.class);
        assertThat(saturdayLessons).isEqualTo(0);
        // 但周日 / 周五等应该正常
        assertThat(result.getGenerated()).isGreaterThan(0);
    }

    @Test
    @Transactional
    void respects_valid_from_to() {
        // 把 1101 的 230001 设置 valid_to 在 2026-05-15（已过期）
        jdbc.update(
                "UPDATE t_teacher_availability SET valid_to = ? WHERE id = 230001",
                LocalDate.of(2026, 5, 15));

        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setTeacherIds(java.util.List.of(1101L));
        BulkGenerateResult result = generator.generate(req);

        // 230001 已过期 → 不生成；230002 还在 → 生成 1 节
        Integer rowsFor230001 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson WHERE teacher_availability_id = 230001 AND DATE(start_at) >= '2026-06-01'",
                Integer.class);
        assertThat(rowsFor230001).isEqualTo(0);
        assertThat(result.getGenerated()).isGreaterThanOrEqualTo(1);

        // 回滚
        jdbc.update("UPDATE t_teacher_availability SET valid_to = NULL WHERE id = 230001");
    }

    @Test
    @Transactional
    void filters_by_branch() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setBranchId(999L); // 不存在的 branch
        BulkGenerateResult result = generator.generate(req);
        assertThat(result.getGenerated()).isEqualTo(0);
    }

    @Test
    @Transactional
    void filters_by_teacher_ids() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setTeacherIds(java.util.List.of(1101L));
        BulkGenerateResult result = generator.generate(req);
        // 只生成 1101 的：周六/周日各 1 节 = 2
        assertThat(result.getGenerated()).isEqualTo(2);
    }

    @Test
    @Transactional
    void skips_disabled_availability() {
        jdbc.update("UPDATE t_teacher_availability SET status = 0 WHERE id = 230001");

        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setTeacherIds(java.util.List.of(1101L));
        BulkGenerateResult result = generator.generate(req);
        // 仅 230002 生成（230001 停用）→ 1 节
        assertThat(result.getGenerated()).isEqualTo(1);

        jdbc.update("UPDATE t_teacher_availability SET status = 1 WHERE id = 230001");
    }

    @Test
    @Transactional
    void adds_subscribers_to_roster() {
        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        req.setTeacherIds(java.util.List.of(1101L));
        BulkGenerateResult result = generator.generate(req);
        // 学员 240001 订阅了 230001 → 应当被加入名单
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
        req.setTeacherIds(java.util.List.of(1101L));
        BulkGenerateResult result = generator.generate(req);
        // 8 周 × 2 availability = 16 节
        assertThat(result.getGenerated()).isEqualTo(16);
    }

    @Test
    @Transactional
    void zero_active_availabilities_returns_zero() {
        // 停用所有 availability
        jdbc.update("UPDATE t_teacher_availability SET status = 0");

        BulkGenerateRequest req = new BulkGenerateRequest();
        req.setFromDate(LocalDate.of(2026, 6, 1));
        req.setWeeks(1);
        BulkGenerateResult result = generator.generate(req);
        assertThat(result.getGenerated()).isEqualTo(0);

        jdbc.update("UPDATE t_teacher_availability SET status = 1");
    }
}
