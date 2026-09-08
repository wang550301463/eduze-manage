package com.eduze.manage.student.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduze.manage.attendance.domain.Attendance;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.domain.ClassRoom;
import com.eduze.manage.course.domain.Course;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.course.mapper.ClassRoomMapper;
import com.eduze.manage.course.mapper.CourseMapper;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.domain.LessonSource;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.domain.StudentLessonHistory;
import com.eduze.manage.student.dto.StudentLessonHistoryResponse;
import com.eduze.manage.student.mapper.StudentLessonHistoryMapper;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.manage.tenant.TenantContext;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 学员历史课程快照：签到成功后追加写入，重复签到幂等忽略。 */
@Service
@RequiredArgsConstructor
public class StudentLessonHistoryService {

    private final StudentLessonHistoryMapper historyMapper;
    private final ClassGroupMapper classGroupMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final ClassRoomMapper classRoomMapper;
    private final BranchAccessGuard branchAccessGuard;

    @Transactional
    public void recordFromCheckIn(Student student, Lesson lesson, Attendance attendance) {
        if (student == null || lesson == null) {
            return;
        }

        ClassGroup classGroup =
                lesson.getClassGroupId() != null
                        ? classGroupMapper.selectById(lesson.getClassGroupId())
                        : null;
        Course course =
                classGroup != null && classGroup.getCourseId() != null
                        ? courseMapper.selectById(classGroup.getCourseId())
                        : null;
        User teacher =
                lesson.getTeacherId() != null ? userMapper.selectById(lesson.getTeacherId()) : null;
        ClassRoom classRoom =
                lesson.getClassRoomId() != null
                        ? classRoomMapper.selectById(lesson.getClassRoomId())
                        : null;

        Integer minutes = null;
        if (lesson.getStartAt() != null && lesson.getEndAt() != null) {
            minutes =
                    (int)
                            Math.max(
                                    1,
                                    Duration.between(lesson.getStartAt(), lesson.getEndAt())
                                            .toMinutes());
        }

        LocalDateTime occurredAt =
                attendance != null && attendance.getCheckInAt() != null
                        ? attendance.getCheckInAt()
                        : (lesson.getStartAt() != null ? lesson.getStartAt() : LocalDateTime.now());

        StudentLessonHistory row = new StudentLessonHistory();
        row.setTenantId(
                TenantContext.getTenantId() != null
                        ? TenantContext.getTenantId()
                        : student.getTenantId());
        row.setBranchId(
                student.getBranchId() != null ? student.getBranchId() : lesson.getBranchId());
        row.setStudentId(student.getId());
        row.setStudentName(student.getName() != null ? student.getName() : "");
        row.setLessonId(lesson.getId());
        row.setAttendanceId(attendance != null ? attendance.getId() : null);
        row.setCourseId(
                course != null
                        ? course.getId()
                        : (classGroup != null ? classGroup.getCourseId() : null));
        row.setCourseName(course != null ? course.getName() : null);
        row.setClassGroupId(lesson.getClassGroupId());
        row.setClassGroupName(classGroup != null ? classGroup.getName() : null);
        row.setTeacherId(lesson.getTeacherId());
        row.setTeacherName(teacher != null ? teacher.getName() : null);
        row.setClassRoomId(lesson.getClassRoomId());
        row.setClassRoomName(classRoom != null ? classRoom.getName() : null);
        row.setStartAt(lesson.getStartAt());
        row.setEndAt(lesson.getEndAt());
        row.setSource(lesson.getSource());
        row.setAttendanceStatus(attendance != null ? attendance.getStatus() : null);
        row.setMinutes(minutes);
        row.setOccurredAt(occurredAt);

        try {
            historyMapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            // 幂等：同一学员+课次已有快照则忽略
        }
    }

    public Page<StudentLessonHistoryResponse> list(
            Long studentId, Long branchId, LocalDate from, LocalDate to, int page, int size) {
        if (branchId != null) {
            branchAccessGuard.requireBranchAccess(branchId);
        }
        LocalDateTime fromAt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toAt = to != null ? to.atTime(LocalTime.MAX) : null;

        Page<StudentLessonHistory> result =
                historyMapper.selectPage(
                        new Page<>(page, size),
                        Wrappers.<StudentLessonHistory>lambdaQuery()
                                .eq(StudentLessonHistory::getTenantId, TenantContext.getTenantId())
                                .eq(
                                        studentId != null,
                                        StudentLessonHistory::getStudentId,
                                        studentId)
                                .eq(branchId != null, StudentLessonHistory::getBranchId, branchId)
                                .ge(fromAt != null, StudentLessonHistory::getOccurredAt, fromAt)
                                .le(toAt != null, StudentLessonHistory::getOccurredAt, toAt)
                                .orderByDesc(StudentLessonHistory::getOccurredAt)
                                .orderByDesc(StudentLessonHistory::getId));

        Page<StudentLessonHistoryResponse> mapped =
                new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        mapped.setRecords(result.getRecords().stream().map(this::toResponse).toList());
        return mapped;
    }

    private StudentLessonHistoryResponse toResponse(StudentLessonHistory row) {
        return StudentLessonHistoryResponse.builder()
                .id(row.getId())
                .branchId(row.getBranchId())
                .studentId(row.getStudentId())
                .studentName(row.getStudentName())
                .lessonId(row.getLessonId())
                .attendanceId(row.getAttendanceId())
                .courseId(row.getCourseId())
                .courseName(row.getCourseName())
                .classGroupId(row.getClassGroupId())
                .classGroupName(row.getClassGroupName())
                .teacherId(row.getTeacherId())
                .teacherName(row.getTeacherName())
                .classRoomId(row.getClassRoomId())
                .classRoomName(row.getClassRoomName())
                .startAt(row.getStartAt())
                .endAt(row.getEndAt())
                .source(row.getSource())
                .sourceLabel(LessonSource.label(row.getSource()))
                .attendanceStatus(row.getAttendanceStatus())
                .minutes(row.getMinutes())
                .snapshotJson(row.getSnapshotJson())
                .occurredAt(row.getOccurredAt())
                .createdAt(row.getCreatedAt())
                .build();
    }
}
