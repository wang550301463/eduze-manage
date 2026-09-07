package com.eduze.manage.lesson.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.domain.LessonStatus;
import com.eduze.manage.lesson.dto.ConflictCheckRequest;
import com.eduze.manage.lesson.dto.ConflictReport;
import com.eduze.manage.lesson.dto.LessonResponse;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import com.eduze.manage.teacher.mapper.TeacherAvailabilityMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConflictService {

    private final LessonMapper lessonMapper;
    private final LessonAssembler lessonAssembler;
    private final ClassGroupMapper classGroupMapper;
    private final TeacherAvailabilityMapper teacherAvailabilityMapper;

    public record LessonDraft(
            Long lessonId,
            Long branchId,
            Long classGroupId,
            Long classRoomId,
            Long teacherId,
            LocalDateTime startAt,
            LocalDateTime endAt) {}

    public ConflictReport check(LessonDraft draft) {
        LessonResponse teacher = draft.teacherId() != null
                ? findOverlap(draft, Wrappers.<Lesson>lambdaQuery()
                        .eq(Lesson::getTeacherId, draft.teacherId()))
                : null;
        LessonResponse classRoom = draft.classRoomId() != null
                ? findOverlap(draft, Wrappers.<Lesson>lambdaQuery()
                        .eq(Lesson::getClassRoomId, draft.classRoomId()))
                : null;
        LessonResponse classGroup = draft.classGroupId() != null
                ? findOverlap(
                        draft, Wrappers.<Lesson>lambdaQuery().eq(Lesson::getClassGroupId, draft.classGroupId()))
                : null;
        boolean hasConflict = teacher != null || classRoom != null || classGroup != null;
        return ConflictReport.builder()
                .hasConflict(hasConflict)
                .teacher(teacher)
                .classRoom(classRoom)
                .classGroup(classGroup)
                .boundAvailability(null)
                .build();
    }

    /**
     * 特殊课硬冲突：老师时间与任一已绑分组 availability 在当天展开窗口重叠则冲突。
     */
    public ConflictReport checkSpecialAgainstBoundWindows(LessonDraft draft) {
        ConflictReport base = check(draft);
        LessonResponse boundSlot = findBoundAvailabilityOverlap(draft);
        boolean hasConflict = base.isHasConflict() || boundSlot != null;
        return ConflictReport.builder()
                .hasConflict(hasConflict)
                .teacher(base.getTeacher())
                .classRoom(base.getClassRoom())
                .classGroup(base.getClassGroup())
                .boundAvailability(boundSlot)
                .build();
    }

    public ConflictReport checkRequest(ConflictCheckRequest request) {
        return check(new LessonDraft(
                request.getLessonId(),
                request.getBranchId(),
                request.getClassGroupId(),
                request.getClassRoomId(),
                request.getTeacherId(),
                request.getStartAt(),
                request.getEndAt()));
    }

    private LessonResponse findBoundAvailabilityOverlap(LessonDraft draft) {
        if (draft.teacherId() == null || draft.startAt() == null || draft.endAt() == null) {
            return null;
        }
        int dayOfWeek = draft.startAt().getDayOfWeek().getValue();
        int startMinute = draft.startAt().getHour() * 60 + draft.startAt().getMinute();
        int endMinute = draft.endAt().getHour() * 60 + draft.endAt().getMinute();
        if (draft.endAt().toLocalDate().isAfter(draft.startAt().toLocalDate())) {
            endMinute = 24 * 60;
        }

        List<ClassGroup> boundGroups = classGroupMapper.selectList(Wrappers.<ClassGroup>lambdaQuery()
                .eq(ClassGroup::getTenantId, TenantContext.getTenantId())
                .isNotNull(ClassGroup::getTeacherAvailabilityId));
        if (boundGroups.isEmpty()) {
            return null;
        }
        Set<Long> availIds = boundGroups.stream()
                .map(ClassGroup::getTeacherAvailabilityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<TeacherAvailability> avails = teacherAvailabilityMapper.selectList(
                Wrappers.<TeacherAvailability>lambdaQuery()
                        .in(TeacherAvailability::getId, availIds)
                        .eq(TeacherAvailability::getTeacherId, draft.teacherId())
                        .eq(TeacherAvailability::getDayOfWeek, dayOfWeek)
                        .eq(TeacherAvailability::getStatus, 1));
        for (TeacherAvailability avail : avails) {
            if (avail.getStartMinute() < endMinute && startMinute < avail.getEndMinute()) {
                return LessonResponse.builder()
                        .id(avail.getId())
                        .branchId(avail.getBranchId())
                        .teacherId(avail.getTeacherId())
                        .startAt(draft.startAt().toLocalDate().atStartOfDay().plusMinutes(avail.getStartMinute()))
                        .endAt(draft.startAt().toLocalDate().atStartOfDay().plusMinutes(avail.getEndMinute()))
                        .build();
            }
        }
        return null;
    }

    private LessonResponse findOverlap(
            LessonDraft draft, com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Lesson> scoped) {
        Lesson conflict = lessonMapper.selectOne(scoped
                .eq(Lesson::getTenantId, TenantContext.getTenantId())
                .eq(Lesson::getBranchId, draft.branchId())
                .ne(Lesson::getStatus, LessonStatus.CANCELLED)
                .lt(Lesson::getStartAt, draft.endAt())
                .gt(Lesson::getEndAt, draft.startAt())
                .ne(draft.lessonId() != null, Lesson::getId, draft.lessonId())
                .last("LIMIT 1"));
        return conflict != null ? lessonAssembler.toResponse(conflict) : null;
    }
}
