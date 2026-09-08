package com.eduze.manage.attendance.service;

import com.eduze.manage.attendance.domain.AttendanceStatus;
import com.eduze.manage.attendance.dto.BranchAttendanceStat;
import com.eduze.manage.attendance.dto.ClassGroupAttendanceStat;
import com.eduze.manage.attendance.dto.StudentAttendanceStat;
import com.eduze.manage.tenant.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttendanceStatService {

    private final JdbcTemplate jdbcTemplate;

    public StudentAttendanceStat studentStat(Long studentId, LocalDate from, LocalDate to) {
        LocalDate[] range = defaultRange(from, to);
        return aggregate(
                countRows(
                        """
                SELECT a.status, COUNT(*) AS cnt
                FROM t_attendance a
                WHERE a.tenant_id = ? AND a.student_id = ? AND a.deleted_at = 0
                  AND a.check_in_at >= ? AND a.check_in_at < ?
                GROUP BY a.status
                """,
                        TenantContext.getTenantId(),
                        studentId,
                        range[0].atStartOfDay(),
                        range[1].plusDays(1).atStartOfDay()));
    }

    public ClassGroupAttendanceStat classGroupStat(
            Long classGroupId, LocalDate from, LocalDate to) {
        LocalDate[] range = defaultRange(from, to);
        StudentAttendanceStat stat =
                aggregate(
                        countRows(
                                """
                SELECT a.status, COUNT(*) AS cnt
                FROM t_attendance a
                INNER JOIN t_lesson l ON l.id = a.lesson_id AND l.deleted_at = 0
                WHERE a.tenant_id = ? AND l.class_group_id = ? AND a.deleted_at = 0
                  AND l.start_at >= ? AND l.start_at < ?
                GROUP BY a.status
                """,
                                TenantContext.getTenantId(),
                                classGroupId,
                                range[0].atStartOfDay(),
                                range[1].plusDays(1).atStartOfDay()));
        String name =
                jdbcTemplate.queryForObject(
                        "SELECT name FROM t_class_group WHERE id = ?", String.class, classGroupId);
        return ClassGroupAttendanceStat.builder()
                .classGroupId(classGroupId)
                .classGroupName(name)
                .total(stat.getTotal())
                .present(stat.getPresent())
                .absent(stat.getAbsent())
                .leave(stat.getLeave())
                .rate(stat.getRate())
                .build();
    }

    public BranchAttendanceStat branchStat(Long branchId, LocalDate from, LocalDate to) {
        LocalDate[] range = defaultRange(from, to);
        StudentAttendanceStat overall =
                aggregate(
                        countRows(
                                """
                SELECT a.status, COUNT(*) AS cnt
                FROM t_attendance a
                WHERE a.tenant_id = ? AND a.branch_id = ? AND a.deleted_at = 0
                  AND a.check_in_at >= ? AND a.check_in_at < ?
                GROUP BY a.status
                """,
                                TenantContext.getTenantId(),
                                branchId,
                                range[0].atStartOfDay(),
                                range[1].plusDays(1).atStartOfDay()));

        List<Map<String, Object>> perClass =
                jdbcTemplate.queryForList(
                        """
                SELECT l.class_group_id AS cg_id, a.status, COUNT(*) AS cnt
                FROM t_attendance a
                INNER JOIN t_lesson l ON l.id = a.lesson_id AND l.deleted_at = 0
                WHERE a.tenant_id = ? AND a.branch_id = ? AND a.deleted_at = 0
                  AND l.start_at >= ? AND l.start_at < ?
                GROUP BY l.class_group_id, a.status
                """,
                        TenantContext.getTenantId(),
                        branchId,
                        range[0].atStartOfDay(),
                        range[1].plusDays(1).atStartOfDay());

        Map<Long, Map<Integer, Integer>> grouped = new java.util.HashMap<>();
        for (Map<String, Object> row : perClass) {
            Object cgObj = row.get("cg_id");
            if (cgObj == null) {
                continue;
            }
            Long cgId = ((Number) cgObj).longValue();
            int status = ((Number) row.get("status")).intValue();
            int cnt = ((Number) row.get("cnt")).intValue();
            grouped.computeIfAbsent(cgId, k -> new java.util.HashMap<>()).put(status, cnt);
        }

        List<ClassGroupAttendanceStat> classStats = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            classStats.add(classGroupStat(entry.getKey(), range[0], range[1]));
        }

        return BranchAttendanceStat.builder()
                .branchId(branchId)
                .total(overall.getTotal())
                .present(overall.getPresent())
                .absent(overall.getAbsent())
                .leave(overall.getLeave())
                .rate(overall.getRate())
                .classGroups(classStats)
                .build();
    }

    public Map<String, Object> dashboardKpis(Long branchId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        YearMonth month = YearMonth.from(today);

        Integer weekAbsent =
                jdbcTemplate.queryForObject(
                        """
                SELECT COUNT(*) FROM t_attendance
                WHERE tenant_id = ? AND branch_id = ? AND deleted_at = 0
                  AND status = ? AND check_in_at >= ?
                """,
                        Integer.class,
                        TenantContext.getTenantId(),
                        branchId,
                        AttendanceStatus.ABSENT.getCode(),
                        weekStart.atStartOfDay());

        BranchAttendanceStat monthStat = branchStat(branchId, month.atDay(1), month.atEndOfMonth());

        Integer weekNewStudents =
                jdbcTemplate.queryForObject(
                        """
                SELECT COUNT(*) FROM t_student
                WHERE tenant_id = ? AND branch_id = ? AND deleted_at = 0
                  AND enroll_date >= ?
                """,
                        Integer.class,
                        TenantContext.getTenantId(),
                        branchId,
                        weekStart);

        Integer weekLessons =
                jdbcTemplate.queryForObject(
                        """
                SELECT COUNT(*) FROM t_lesson
                WHERE tenant_id = ? AND branch_id = ? AND deleted_at = 0
                  AND start_at >= ? AND start_at < ?
                """,
                        Integer.class,
                        TenantContext.getTenantId(),
                        branchId,
                        weekStart.atStartOfDay(),
                        weekStart.plusDays(7).atStartOfDay());

        return Map.of(
                "weekAbsent", weekAbsent != null ? weekAbsent : 0,
                "monthAttendanceRate", monthStat.getRate(),
                "weekNewStudents", weekNewStudents != null ? weekNewStudents : 0,
                "weekLessons", weekLessons != null ? weekLessons : 0);
    }

    private List<Map<String, Object>> countRows(String sql, Object... args) {
        return jdbcTemplate.queryForList(sql, args);
    }

    private StudentAttendanceStat aggregate(List<Map<String, Object>> rows) {
        int present = 0;
        int absent = 0;
        int leave = 0;
        for (Map<String, Object> row : rows) {
            int status = ((Number) row.get("status")).intValue();
            int cnt = ((Number) row.get("cnt")).intValue();
            if (status == AttendanceStatus.CHECKED_IN.getCode()
                    || status == AttendanceStatus.CHECKED_OUT.getCode()) {
                present += cnt;
            } else if (status == AttendanceStatus.ABSENT.getCode()) {
                absent += cnt;
            } else if (status == AttendanceStatus.LEAVE.getCode()) {
                leave += cnt;
            }
        }
        int total = present + absent + leave;
        BigDecimal rate =
                total == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(present)
                                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
        return StudentAttendanceStat.builder()
                .total(total)
                .present(present)
                .absent(absent)
                .leave(leave)
                .rate(rate)
                .build();
    }

    private LocalDate[] defaultRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            YearMonth month = YearMonth.now();
            return new LocalDate[] {month.atDay(1), month.atEndOfMonth()};
        }
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.withDayOfMonth(1);
        return new LocalDate[] {start, end};
    }
}
