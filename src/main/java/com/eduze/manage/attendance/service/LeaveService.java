package com.eduze.manage.attendance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.attendance.domain.Attendance;
import com.eduze.manage.attendance.domain.AttendanceStatus;
import com.eduze.manage.attendance.domain.LeaveRequest;
import com.eduze.manage.attendance.domain.LeaveStatus;
import com.eduze.manage.attendance.dto.LeaveCreateRequest;
import com.eduze.manage.attendance.dto.LeaveResponse;
import com.eduze.manage.attendance.mapper.AttendanceMapper;
import com.eduze.manage.attendance.mapper.LeaveRequestMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestMapper leaveRequestMapper;
    private final AttendanceMapper attendanceMapper;
    private final StudentMapper studentMapper;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public LeaveResponse create(LeaveCreateRequest request) {
        Student student = requireStudent(request.getStudentId());
        LeaveRequest leave = new LeaveRequest();
        leave.setTenantId(TenantContext.getTenantId());
        leave.setBranchId(student.getBranchId());
        leave.setStudentId(student.getId());
        leave.setLessonId(request.getLessonId());
        leave.setLeaveStartDate(request.getLeaveStartDate());
        leave.setLeaveEndDate(request.getLeaveEndDate());
        leave.setReason(request.getReason());
        leave.setStatus(LeaveStatus.PENDING.getCode());
        leaveRequestMapper.insert(leave);
        return toResponse(leave, student);
    }

    public List<LeaveResponse> list(Integer status, Long studentId, LocalDate from, LocalDate to) {
        return leaveRequestMapper.selectList(Wrappers.<LeaveRequest>lambdaQuery()
                        .eq(LeaveRequest::getTenantId, TenantContext.getTenantId())
                        .eq(status != null, LeaveRequest::getStatus, status)
                        .eq(studentId != null, LeaveRequest::getStudentId, studentId)
                        .ge(from != null, LeaveRequest::getLeaveStartDate, from)
                        .le(to != null, LeaveRequest::getLeaveEndDate, to)
                        .orderByDesc(LeaveRequest::getCreatedAt))
                .stream()
                .map(l -> toResponse(l, studentMapper.selectById(l.getStudentId())))
                .toList();
    }

    @Transactional
    public LeaveResponse approve(Long id) {
        LeaveRequest leave = requireLeave(id);
        if (leave.getStatus() != LeaveStatus.PENDING.getCode()) {
            throw new BizException(ErrorCode.CONFLICT, "请假单状态不可审批");
        }
        leave.setStatus(LeaveStatus.APPROVED.getCode());
        leave.setApprovedAt(LocalDateTime.now());
        leave.setApprovedBy(currentUserId());
        leaveRequestMapper.updateById(leave);
        markAttendancesAsLeave(leave);
        return toResponse(leave, requireStudent(leave.getStudentId()));
    }

    @Transactional
    public LeaveResponse reject(Long id) {
        LeaveRequest leave = requireLeave(id);
        if (leave.getStatus() != LeaveStatus.PENDING.getCode()) {
            throw new BizException(ErrorCode.CONFLICT, "请假单状态不可审批");
        }
        leave.setStatus(LeaveStatus.REJECTED.getCode());
        leave.setApprovedAt(LocalDateTime.now());
        leave.setApprovedBy(currentUserId());
        leaveRequestMapper.updateById(leave);
        return toResponse(leave, requireStudent(leave.getStudentId()));
    }

    private void markAttendancesAsLeave(LeaveRequest leave) {
        jdbcTemplate.update(
                """
                UPDATE t_attendance a
                INNER JOIN t_lesson l ON l.id = a.lesson_id AND l.deleted_at = 0
                SET a.status = ?, a.updated_at = NOW(3)
                WHERE a.student_id = ? AND a.tenant_id = ? AND a.deleted_at = 0
                  AND DATE(l.start_at) BETWEEN ? AND ?
                """,
                AttendanceStatus.LEAVE.getCode(),
                leave.getStudentId(),
                TenantContext.getTenantId(),
                leave.getLeaveStartDate(),
                leave.getLeaveEndDate());

        List<Long> lessonIds = jdbcTemplate.queryForList(
                """
                SELECT l.id FROM t_lesson l
                INNER JOIN t_student_class_group scg ON scg.class_group_id = l.class_group_id
                    AND scg.student_id = ? AND scg.deleted_at = 0 AND scg.left_at IS NULL
                WHERE l.tenant_id = ? AND l.deleted_at = 0
                  AND DATE(l.start_at) BETWEEN ? AND ?
                  AND NOT EXISTS (
                      SELECT 1 FROM t_attendance a
                      WHERE a.lesson_id = l.id AND a.student_id = ? AND a.deleted_at = 0
                  )
                """,
                Long.class,
                leave.getStudentId(),
                TenantContext.getTenantId(),
                leave.getLeaveStartDate(),
                leave.getLeaveEndDate(),
                leave.getStudentId());
        for (Long lessonId : lessonIds) {
            var lesson = jdbcTemplate.queryForMap(
                    "SELECT branch_id FROM t_lesson WHERE id = ?", lessonId);
            Attendance row = new Attendance();
            row.setTenantId(TenantContext.getTenantId());
            row.setBranchId(((Number) lesson.get("branch_id")).longValue());
            row.setLessonId(lessonId);
            row.setStudentId(leave.getStudentId());
            row.setStatus(AttendanceStatus.LEAVE.getCode());
            row.setCheckInMethod("auto");
            attendanceMapper.insert(row);
        }
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LeaveRequest requireLeave(Long id) {
        LeaveRequest leave = leaveRequestMapper.selectById(id);
        if (leave == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "请假单不存在");
        }
        return leave;
    }

    private Student requireStudent(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        }
        return student;
    }

    private LeaveResponse toResponse(LeaveRequest leave, Student student) {
        String statusLabel = switch (leave.getStatus()) {
            case 1 -> "待审批";
            case 2 -> "已批准";
            case 3 -> "已拒绝";
            default -> "未知";
        };
        return LeaveResponse.builder()
                .id(leave.getId())
                .branchId(leave.getBranchId())
                .studentId(leave.getStudentId())
                .studentName(student != null ? student.getName() : null)
                .lessonId(leave.getLessonId())
                .leaveStartDate(leave.getLeaveStartDate())
                .leaveEndDate(leave.getLeaveEndDate())
                .reason(leave.getReason())
                .status(leave.getStatus())
                .statusLabel(statusLabel)
                .approvedBy(leave.getApprovedBy())
                .approvedAt(leave.getApprovedAt())
                .createdAt(leave.getCreatedAt())
                .build();
    }
}
