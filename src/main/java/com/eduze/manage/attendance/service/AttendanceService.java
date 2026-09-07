package com.eduze.manage.attendance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.attendance.domain.Attendance;
import com.eduze.manage.attendance.domain.AttendanceStatus;
import com.eduze.manage.attendance.domain.CheckInMethod;
import com.eduze.manage.attendance.domain.PickupRecord;
import com.eduze.manage.attendance.dto.AttendanceResponse;
import com.eduze.manage.attendance.dto.CheckInRequest;
import com.eduze.manage.attendance.dto.CheckOutRequest;
import com.eduze.manage.attendance.dto.TodayRosterResponse;
import com.eduze.manage.attendance.dto.TodayRosterResponse.RosterItem;
import com.eduze.manage.attendance.dto.UpdateAttendanceRequest;
import com.eduze.manage.attendance.mapper.AttendanceMapper;
import com.eduze.manage.attendance.mapper.PickupRecordMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.student.domain.Guardian;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.mapper.GuardianMapper;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceMapper attendanceMapper;
    private final PickupRecordMapper pickupRecordMapper;
    private final LessonMapper lessonMapper;
    private final ClassGroupMapper classGroupMapper;
    private final StudentMapper studentMapper;
    private final GuardianMapper guardianMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AttendanceSupport attendanceSupport;
    private final PickupService pickupService;

    public TodayRosterResponse todayRoster(Long branchId, String period, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalTime start = AttendanceSupport.periodStart(period);
        LocalTime end = AttendanceSupport.periodEnd(period);
        LocalDateTime rangeStart = targetDate.atTime(start);
        LocalDateTime rangeEnd = targetDate.atTime(end);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT l.id AS lesson_id, l.start_at, l.end_at, l.class_group_id,
                       s.id AS student_id, s.name AS student_name,
                       cg.name AS class_group_name,
                       a.id AS attendance_id, a.status, a.check_in_at, a.check_out_at
                FROM t_lesson l
                INNER JOIN t_student_class_group scg ON scg.class_group_id = l.class_group_id
                    AND scg.deleted_at = 0 AND scg.left_at IS NULL
                INNER JOIN t_student s ON s.id = scg.student_id AND s.deleted_at = 0
                INNER JOIN t_class_group cg ON cg.id = l.class_group_id AND cg.deleted_at = 0
                LEFT JOIN t_attendance a ON a.lesson_id = l.id AND a.student_id = s.id AND a.deleted_at = 0
                WHERE l.tenant_id = ? AND l.branch_id = ? AND l.deleted_at = 0
                  AND l.start_at >= ? AND l.start_at < ?
                ORDER BY l.start_at, s.name
                """,
                TenantContext.getTenantId(),
                branchId,
                rangeStart,
                rangeEnd);

        List<RosterItem> items = new ArrayList<>();
        int checkedIn = 0;
        for (Map<String, Object> row : rows) {
            Long studentId = ((Number) row.get("student_id")).longValue();
            Integer status = row.get("status") != null ? ((Number) row.get("status")).intValue() : null;
            LocalDate lessonDate = toLocalDateTime(row.get("start_at")).toLocalDate();
            if (status == null && attendanceSupport.hasApprovedLeave(studentId, lessonDate)) {
                status = AttendanceStatus.LEAVE.getCode();
            }
            if (status != null && (status == AttendanceStatus.CHECKED_IN.getCode()
                    || status == AttendanceStatus.CHECKED_OUT.getCode())) {
                checkedIn++;
            }
            items.add(RosterItem.builder()
                    .lessonId(((Number) row.get("lesson_id")).longValue())
                    .studentId(studentId)
                    .studentName((String) row.get("student_name"))
                    .classGroupName((String) row.get("class_group_name"))
                    .lessonStartAt(toLocalDateTime(row.get("start_at")))
                    .attendanceId(row.get("attendance_id") != null
                            ? ((Number) row.get("attendance_id")).longValue()
                            : null)
                    .status(status)
                    .statusLabel(AttendanceSupport.statusLabel(status))
                    .checkInAt(row.get("check_in_at") != null ? toLocalDateTime(row.get("check_in_at")) : null)
                    .checkOutAt(row.get("check_out_at") != null ? toLocalDateTime(row.get("check_out_at")) : null)
                    .build());
        }
        return TodayRosterResponse.builder()
                .items(items)
                .totalExpected(items.size())
                .checkedInCount(checkedIn)
                .build();
    }

    @Transactional
    public AttendanceResponse checkIn(CheckInRequest request) {
        QrResolve qrResolve = resolveQrContext(request);
        Lesson lesson = qrResolve.lesson();
        Student student = qrResolve.student();
        Long guardianId = qrResolve.guardianId();

        if (attendanceSupport.hasApprovedLeave(student.getId(), lesson.getStartAt().toLocalDate())) {
            Attendance existing = findAttendance(lesson.getId(), student.getId());
            if (existing != null) {
                existing.setStatus(AttendanceStatus.LEAVE.getCode());
                attendanceMapper.updateById(existing);
                return toResponse(existing, student, lesson);
            }
            Attendance leaveRow = buildAttendance(lesson, student, AttendanceStatus.LEAVE.getCode(), null);
            leaveRow.setCheckInMethod(CheckInMethod.MANUAL);
            attendanceMapper.insert(leaveRow);
            return toResponse(leaveRow, student, lesson);
        }

        Attendance attendance = buildAttendance(
                lesson, student, AttendanceStatus.CHECKED_IN.getCode(), resolveMethod(request.getMethod()));
        attendance.setCheckInAt(LocalDateTime.now());
        try {
            attendanceMapper.insert(attendance);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "学员已签到", ex);
        }

        pickupService.recordPickupInternal(
                attendance, "in", guardianId, null, null, LocalDateTime.now());
        return toResponse(attendance, student, lesson);
    }

    @Transactional
    public AttendanceResponse checkOut(CheckOutRequest request) {
        Attendance attendance = requireAttendance(request.getAttendanceId());
        if (attendance.getStatus() != AttendanceStatus.CHECKED_IN.getCode()) {
            throw new BizException(ErrorCode.CONFLICT, "当前状态不可离园");
        }
        Student student = requireStudent(attendance.getStudentId());
        Lesson lesson = requireLesson(attendance.getLessonId());

        attendance.setStatus(AttendanceStatus.CHECKED_OUT.getCode());
        attendance.setCheckOutAt(LocalDateTime.now());
        attendanceMapper.updateById(attendance);

        boolean abnormal = request.getIsAbnormal() != null && request.getIsAbnormal();
        if (!abnormal && request.getGuardianId() != null
                && !attendanceSupport.isAuthorizedPickup(student.getId(), request.getGuardianId())) {
            abnormal = true;
        }
        pickupService.recordPickupInternal(
                attendance,
                "out",
                request.getGuardianId(),
                abnormal,
                request.getAbnormalNote(),
                LocalDateTime.now());
        return toResponse(attendance, student, lesson);
    }

    @Transactional
    public AttendanceResponse updateStatus(Long id, UpdateAttendanceRequest request) {
        Attendance attendance = requireAttendance(id);
        if (request.getStatus() != AttendanceStatus.ABSENT.getCode()
                && request.getStatus() != AttendanceStatus.LEAVE.getCode()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "仅支持更正为缺勤或请假");
        }
        attendance.setStatus(request.getStatus());
        attendance.setNote(request.getNote());
        attendanceMapper.updateById(attendance);
        return toResponse(
                attendance,
                requireStudent(attendance.getStudentId()),
                requireLesson(attendance.getLessonId()));
    }

    public List<AttendanceResponse> listByStudent(Long studentId, LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate end = to != null ? to : LocalDate.now();
        List<Attendance> rows = attendanceMapper.selectList(Wrappers.<Attendance>lambdaQuery()
                .eq(Attendance::getTenantId, TenantContext.getTenantId())
                .eq(Attendance::getStudentId, studentId)
                .ge(Attendance::getCheckInAt, start.atStartOfDay())
                .le(Attendance::getCheckInAt, end.plusDays(1).atStartOfDay())
                .orderByDesc(Attendance::getCheckInAt));
        Map<Long, Lesson> lessonCache = new HashMap<>();
        Map<Long, Student> studentCache = new HashMap<>();
        Student student = requireStudent(studentId);
        studentCache.put(studentId, student);
        return rows.stream()
                .map(a -> {
                    Lesson lesson = lessonCache.computeIfAbsent(
                            a.getLessonId(), id -> requireLesson(id));
                    return toResponse(a, studentCache.get(a.getStudentId()), lesson);
                })
                .toList();
    }

    private QrResolve resolveQrContext(CheckInRequest request) {
        if (CheckInMethod.QR.equals(request.getMethod())) {
            if (request.getQrCode() == null || request.getQrCode().isBlank()) {
                throw new BizException(ErrorCode.VALIDATION_FAILED, "二维码内容不能为空");
            }
            Guardian guardian = guardianMapper.selectOne(Wrappers.<Guardian>lambdaQuery()
                    .eq(Guardian::getTenantId, TenantContext.getTenantId())
                    .eq(Guardian::getQrCode, request.getQrCode()));
            if (guardian == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "未找到对应家长");
            }
            Long studentId = request.getStudentId();
            if (studentId == null) {
                studentId = jdbcTemplate.query(
                        """
                        SELECT student_id FROM t_student_guardian_relation
                        WHERE tenant_id = ? AND guardian_id = ? AND deleted_at = 0
                        LIMIT 1
                        """,
                        rs -> rs.next() ? rs.getLong(1) : null,
                        TenantContext.getTenantId(),
                        guardian.getId());
                if (studentId == null) {
                    throw new BizException(ErrorCode.NOT_FOUND, "该家长未关联学员");
                }
            }
            Student student = requireStudent(studentId);
            Long lessonId = request.getLessonId();
            Lesson lesson;
            if (lessonId == null) {
                var lessons = jdbcTemplate.query(
                        """
                        SELECT l.id, l.tenant_id, l.branch_id, l.class_group_id, l.class_room_id,
                               l.teacher_id, l.start_at, l.end_at, l.status, l.note
                        FROM t_lesson l
                        INNER JOIN t_student_class_group scg ON scg.class_group_id = l.class_group_id
                            AND scg.student_id = ? AND scg.deleted_at = 0 AND scg.left_at IS NULL
                        WHERE l.tenant_id = ? AND l.deleted_at = 0
                          AND DATE(l.start_at) = CURDATE()
                        ORDER BY l.start_at
                        LIMIT 1
                        """,
                        (rs, rowNum) -> {
                            Lesson l = new Lesson();
                            l.setId(rs.getLong("id"));
                            l.setTenantId(rs.getLong("tenant_id"));
                            l.setBranchId(rs.getLong("branch_id"));
                            l.setClassGroupId(rs.getLong("class_group_id"));
                            l.setClassRoomId(rs.getObject("class_room_id", Long.class));
                            l.setTeacherId(rs.getObject("teacher_id", Long.class));
                            l.setStartAt(rs.getTimestamp("start_at").toLocalDateTime());
                            l.setEndAt(rs.getTimestamp("end_at").toLocalDateTime());
                            l.setStatus(rs.getString("status"));
                            l.setNote(rs.getString("note"));
                            return l;
                        },
                        studentId,
                        TenantContext.getTenantId());
                if (lessons.isEmpty()) {
                    throw new BizException(ErrorCode.NOT_FOUND, "今日无该学员课次");
                }
                lesson = lessons.get(0);
            } else {
                lesson = requireLesson(lessonId);
            }
            return new QrResolve(lesson, student, guardian.getId());
        }
        if (request.getLessonId() == null || request.getStudentId() == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "课次与学员不能为空");
        }
        return new QrResolve(
                requireLesson(request.getLessonId()),
                requireStudent(request.getStudentId()),
                request.getGuardianId());
    }

    private record QrResolve(Lesson lesson, Student student, Long guardianId) {}

    private String resolveMethod(String method) {
        if (CheckInMethod.QR.equals(method)) {
            return CheckInMethod.QR;
        }
        if (CheckInMethod.MANUAL.equals(method)) {
            return CheckInMethod.MANUAL;
        }
        throw new BizException(ErrorCode.VALIDATION_FAILED, "不支持的签到方式");
    }

    private Attendance findAttendance(Long lessonId, Long studentId) {
        return attendanceMapper.selectOne(Wrappers.<Attendance>lambdaQuery()
                .eq(Attendance::getTenantId, TenantContext.getTenantId())
                .eq(Attendance::getLessonId, lessonId)
                .eq(Attendance::getStudentId, studentId));
    }

    private Attendance buildAttendance(Lesson lesson, Student student, int status, String method) {
        Attendance attendance = new Attendance();
        attendance.setTenantId(TenantContext.getTenantId());
        attendance.setBranchId(lesson.getBranchId());
        attendance.setLessonId(lesson.getId());
        attendance.setStudentId(student.getId());
        attendance.setStatus(status);
        attendance.setCheckInMethod(method);
        return attendance;
    }

    private Attendance requireAttendance(Long id) {
        Attendance attendance = attendanceMapper.selectById(id);
        if (attendance == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "出勤记录不存在");
        }
        return attendance;
    }

    private Lesson requireLesson(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课次不存在");
        }
        return lesson;
    }

    private Student requireStudent(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        }
        return student;
    }

    private AttendanceResponse toResponse(Attendance attendance, Student student, Lesson lesson) {
        ClassGroup classGroup = classGroupMapper.selectById(lesson.getClassGroupId());
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .lessonId(attendance.getLessonId())
                .studentId(attendance.getStudentId())
                .studentName(student.getName())
                .classGroupName(classGroup != null ? classGroup.getName() : null)
                .status(attendance.getStatus())
                .statusLabel(AttendanceSupport.statusLabel(attendance.getStatus()))
                .checkInAt(attendance.getCheckInAt())
                .checkOutAt(attendance.getCheckOutAt())
                .checkInMethod(attendance.getCheckInMethod())
                .note(attendance.getNote())
                .lessonStartAt(lesson.getStartAt())
                .lessonEndAt(lesson.getEndAt())
                .build();
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        throw new IllegalArgumentException("Unsupported datetime type: " + value.getClass());
    }
}
