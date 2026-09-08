package com.eduze.manage.lesson.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.course.service.ClassGroupService;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.domain.LessonChangeLog;
import com.eduze.manage.lesson.domain.LessonSource;
import com.eduze.manage.lesson.domain.LessonStatus;
import com.eduze.manage.lesson.dto.BulkGenerateRequest;
import com.eduze.manage.lesson.dto.BulkGenerateResult;
import com.eduze.manage.lesson.dto.CancelLessonRequest;
import com.eduze.manage.lesson.dto.ConflictCheckRequest;
import com.eduze.manage.lesson.dto.ConflictReport;
import com.eduze.manage.lesson.dto.LessonChangeLogResponse;
import com.eduze.manage.lesson.dto.LessonRequest;
import com.eduze.manage.lesson.dto.LessonResponse;
import com.eduze.manage.lesson.dto.RescheduleRequest;
import com.eduze.manage.lesson.mapper.LessonChangeLogMapper;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.lesson.service.ConflictService.LessonDraft;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LessonService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final LessonMapper lessonMapper;
    private final LessonChangeLogMapper lessonChangeLogMapper;
    private final ClassGroupMapper classGroupMapper;
    private final ClassGroupService classGroupService;
    private final ConflictService conflictService;
    private final LessonAssembler lessonAssembler;
    private final ScheduleGenerator scheduleGenerator;
    private final BranchAccessGuard branchAccessGuard;

    public List<LessonResponse> list(
            Long branchId,
            Long classGroupId,
            Long teacherId,
            LocalDateTime from,
            LocalDateTime to) {
        if (branchId != null) {
            branchAccessGuard.requireBranchAccess(branchId);
        }
        var query =
                Wrappers.<Lesson>lambdaQuery()
                        .eq(Lesson::getTenantId, TenantContext.getTenantId())
                        .eq(branchId != null, Lesson::getBranchId, branchId)
                        .eq(classGroupId != null, Lesson::getClassGroupId, classGroupId)
                        .eq(teacherId != null, Lesson::getTeacherId, teacherId)
                        .ge(from != null, Lesson::getStartAt, from)
                        .lt(to != null, Lesson::getStartAt, to)
                        .orderByAsc(Lesson::getStartAt);
        return lessonMapper.selectList(query).stream().map(lessonAssembler::toResponse).toList();
    }

    public LessonResponse get(Long id) {
        Lesson lesson = requireLesson(id);
        branchAccessGuard.requireBranchAccess(lesson.getBranchId());
        return lessonAssembler.toResponse(lesson);
    }

    @Transactional
    public LessonResponse create(LessonRequest request) {
        Integer source = request.getSource() != null ? request.getSource() : LessonSource.MANUAL;
        boolean special = LessonSource.isSpecial(source);
        Long branchId = request.getBranchId();
        Long classGroupId = request.getClassGroupId();

        if (!special && classGroupId == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "非特殊课须指定班级分组");
        }
        if (classGroupId != null) {
            ClassGroup group = classGroupService.requireGroup(classGroupId);
            branchId = request.getBranchId() != null ? request.getBranchId() : group.getBranchId();
        }
        branchAccessGuard.requireBranchAccess(branchId);

        LessonDraft draft =
                new LessonDraft(
                        null,
                        branchId,
                        classGroupId,
                        request.getClassRoomId(),
                        request.getTeacherId(),
                        request.getStartAt(),
                        request.getEndAt());
        if (special) {
            assertNoConflict(conflictService.checkSpecialAgainstBoundWindows(draft));
        } else {
            assertNoConflict(draft);
        }

        Lesson lesson = new Lesson();
        lesson.setTenantId(TenantContext.getTenantId());
        lesson.setBranchId(branchId);
        lesson.setClassGroupId(classGroupId);
        lesson.setClassRoomId(request.getClassRoomId());
        lesson.setTeacherId(request.getTeacherId());
        lesson.setStartAt(request.getStartAt());
        lesson.setEndAt(request.getEndAt());
        lesson.setNote(request.getNote());
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setSource(source);
        if (special) {
            lesson.setTeacherAvailabilityId(null);
        }
        lessonMapper.insert(lesson);
        return lessonAssembler.toResponse(lesson);
    }

    @Transactional
    public BulkGenerateResult bulkGenerate(BulkGenerateRequest request) {
        return scheduleGenerator.generate(request);
    }

    @Transactional
    public void delete(Long id) {
        cancel(id, cancelRequest("删除课次"));
    }

    @Transactional
    public LessonResponse reschedule(Long id, RescheduleRequest request) {
        Lesson lesson = requireLesson(id);
        branchAccessGuard.requireBranchAccess(lesson.getBranchId());
        if (LessonStatus.COMPLETED.equals(lesson.getStatus())) {
            throw new BizException(ErrorCode.UNPROCESSABLE, "已完成的课次不能调课");
        }
        Long teacherId =
                request.getTeacherId() != null ? request.getTeacherId() : lesson.getTeacherId();
        Long classRoomId =
                request.getClassRoomId() != null
                        ? request.getClassRoomId()
                        : lesson.getClassRoomId();
        assertNoConflict(
                new LessonDraft(
                        lesson.getId(),
                        lesson.getBranchId(),
                        lesson.getClassGroupId(),
                        classRoomId,
                        teacherId,
                        request.getStartAt(),
                        request.getEndAt()));
        Map<String, Object> before = snapshot(lesson);
        lesson.setStartAt(request.getStartAt());
        lesson.setEndAt(request.getEndAt());
        if (request.getTeacherId() != null) {
            lesson.setTeacherId(request.getTeacherId());
        }
        if (request.getClassRoomId() != null) {
            lesson.setClassRoomId(request.getClassRoomId());
        }
        lessonMapper.updateById(lesson);
        writeChangeLog(lesson.getId(), "RESCHEDULE", before, snapshot(lesson), request.getReason());
        return lessonAssembler.toResponse(lesson);
    }

    @Transactional
    public LessonResponse cancel(Long id, CancelLessonRequest request) {
        Lesson lesson = requireLesson(id);
        Map<String, Object> before = snapshot(lesson);
        lesson.setStatus(LessonStatus.CANCELLED);
        lessonMapper.updateById(lesson);
        writeChangeLog(lesson.getId(), "CANCEL", before, snapshot(lesson), request.getReason());
        return lessonAssembler.toResponse(lesson);
    }

    public ConflictReport checkConflict(ConflictCheckRequest request) {
        return conflictService.checkRequest(request);
    }

    public List<LessonChangeLogResponse> changeLogs(Long lessonId) {
        requireLesson(lessonId);
        return lessonChangeLogMapper
                .selectList(
                        Wrappers.<LessonChangeLog>lambdaQuery()
                                .eq(LessonChangeLog::getTenantId, TenantContext.getTenantId())
                                .eq(LessonChangeLog::getLessonId, lessonId)
                                .orderByDesc(LessonChangeLog::getCreatedAt))
                .stream()
                .map(
                        log ->
                                LessonChangeLogResponse.builder()
                                        .id(log.getId())
                                        .changeType(log.getChangeType())
                                        .beforeJson(log.getBeforeJson())
                                        .afterJson(log.getAfterJson())
                                        .reason(log.getReason())
                                        .operatorId(log.getOperatorId())
                                        .createdAt(log.getCreatedAt())
                                        .build())
                .toList();
    }

    Lesson requireLesson(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课次不存在");
        }
        return lesson;
    }

    private CancelLessonRequest cancelRequest(String reason) {
        CancelLessonRequest req = new CancelLessonRequest();
        req.setReason(reason);
        return req;
    }

    private String buildConflictReason(ConflictReport report) {
        if (report.getBoundAvailability() != null) {
            return "与已绑分组正常时段冲突";
        }
        if (report.getTeacher() != null) {
            return "教师时间冲突";
        }
        if (report.getClassRoom() != null) {
            return "画室时间冲突";
        }
        if (report.getClassGroup() != null) {
            return "班级时间冲突";
        }
        return "时间冲突";
    }

    private void assertNoConflict(LessonDraft draft) {
        ConflictReport report = conflictService.check(draft);
        assertNoConflict(report);
    }

    private void assertNoConflict(ConflictReport report) {
        if (report.isHasConflict()) {
            throw new BizException(ErrorCode.CONFLICT, buildConflictReason(report));
        }
    }

    private Map<String, Object> snapshot(Lesson lesson) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", lesson.getId());
        map.put("startAt", lesson.getStartAt());
        map.put("endAt", lesson.getEndAt());
        map.put("teacherId", lesson.getTeacherId());
        map.put("classRoomId", lesson.getClassRoomId());
        map.put("status", lesson.getStatus());
        return map;
    }

    private void writeChangeLog(
            Long lessonId,
            String changeType,
            Map<String, Object> before,
            Map<String, Object> after,
            String reason) {
        LessonChangeLog log = new LessonChangeLog();
        log.setTenantId(TenantContext.getTenantId());
        log.setLessonId(lessonId);
        log.setChangeType(changeType);
        log.setBeforeJson(JSON.toJSONString(before));
        log.setAfterJson(JSON.toJSONString(after));
        log.setReason(reason);
        log.setOperatorId(currentUserId());
        lessonChangeLogMapper.insert(log);
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUserId();
        }
        return null;
    }
}
