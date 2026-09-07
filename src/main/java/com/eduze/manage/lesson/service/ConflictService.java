package com.eduze.manage.lesson.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.domain.LessonStatus;
import com.eduze.manage.lesson.dto.ConflictCheckRequest;
import com.eduze.manage.lesson.dto.ConflictReport;
import com.eduze.manage.lesson.dto.LessonResponse;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConflictService {

    private final LessonMapper lessonMapper;
    private final LessonAssembler lessonAssembler;

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

    private LessonResponse findOverlap(LessonDraft draft, com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Lesson> scoped) {
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
