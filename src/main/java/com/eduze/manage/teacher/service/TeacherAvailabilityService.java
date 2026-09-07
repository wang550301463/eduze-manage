package com.eduze.manage.teacher.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.domain.LessonStatus;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import com.eduze.manage.teacher.dto.TeacherAvailabilityRequest;
import com.eduze.manage.teacher.dto.TeacherAvailabilityResponse;
import com.eduze.manage.teacher.mapper.TeacherAvailabilityMapper;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherAvailabilityService {

    private final TeacherAvailabilityMapper mapper;
    private final ClassGroupMapper classGroupMapper;
    private final LessonMapper lessonMapper;
    private final UserMapper userMapper;
    private final BranchAccessGuard branchAccessGuard;

    public List<TeacherAvailabilityResponse> list(Long teacherId) {
        LambdaQueryWrapper<TeacherAvailability> wrapper = new LambdaQueryWrapper<TeacherAvailability>()
                .eq(TeacherAvailability::getTeacherId, teacherId)
                .orderByAsc(TeacherAvailability::getDayOfWeek)
                .orderByAsc(TeacherAvailability::getStartMinute);
        return toResponses(mapper.selectList(wrapper));
    }

    /** 本校区启用中、尚未被分组绑定的可用时段（供分组下拉）。 */
    public List<TeacherAvailabilityResponse> listUnbound(Long branchId) {
        branchAccessGuard.requireBranchAccess(branchId);
        List<TeacherAvailability> avails = mapper.selectList(new LambdaQueryWrapper<TeacherAvailability>()
                .eq(TeacherAvailability::getTenantId, TenantContext.getTenantId())
                .eq(TeacherAvailability::getBranchId, branchId)
                .eq(TeacherAvailability::getStatus, 1)
                .orderByAsc(TeacherAvailability::getTeacherId)
                .orderByAsc(TeacherAvailability::getDayOfWeek)
                .orderByAsc(TeacherAvailability::getStartMinute));
        if (avails.isEmpty()) {
            return List.of();
        }
        Set<Long> availIds = avails.stream().map(TeacherAvailability::getId).collect(Collectors.toSet());
        Set<Long> boundIds = classGroupMapper
                .selectList(Wrappers.<ClassGroup>lambdaQuery()
                        .eq(ClassGroup::getTenantId, TenantContext.getTenantId())
                        .in(ClassGroup::getTeacherAvailabilityId, availIds))
                .stream()
                .map(ClassGroup::getTeacherAvailabilityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return toResponses(avails.stream().filter(a -> !boundIds.contains(a.getId())).toList());
    }

    @Transactional
    public TeacherAvailabilityResponse create(Long teacherId, TeacherAvailabilityRequest req) {
        validateTime(req);
        checkOverlap(teacherId, null, req);
        TeacherAvailability entity = new TeacherAvailability();
        entity.setTeacherId(teacherId);
        entity.setBranchId(req.getBranchId());
        entity.setDayOfWeek(req.getDayOfWeek());
        entity.setStartMinute(req.getStartMinute());
        entity.setEndMinute(req.getEndMinute());
        entity.setCapacity(req.getCapacity());
        entity.setDefaultClassRoomId(req.getDefaultClassRoomId());
        entity.setValidFrom(req.getValidFrom());
        entity.setValidTo(req.getValidTo());
        entity.setStatus(req.getStatus());
        entity.setNote(req.getNote());
        mapper.insert(entity);
        return toResponse(entity, null, null);
    }

    @Transactional
    public TeacherAvailabilityResponse update(Long id, TeacherAvailabilityRequest req) {
        TeacherAvailability entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "可用时段不存在");
        }
        validateTime(req);
        checkOverlap(entity.getTeacherId(), id, req);
        entity.setBranchId(req.getBranchId());
        entity.setDayOfWeek(req.getDayOfWeek());
        entity.setStartMinute(req.getStartMinute());
        entity.setEndMinute(req.getEndMinute());
        entity.setCapacity(req.getCapacity());
        entity.setDefaultClassRoomId(req.getDefaultClassRoomId());
        entity.setValidFrom(req.getValidFrom());
        entity.setValidTo(req.getValidTo());
        entity.setStatus(req.getStatus());
        entity.setNote(req.getNote());
        mapper.updateById(entity);
        return toResponse(entity, null, null);
    }

    @Transactional
    public void delete(Long id) {
        TeacherAvailability entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "可用时段不存在");
        }
        ClassGroup bound = classGroupMapper.selectOne(Wrappers.<ClassGroup>lambdaQuery()
                .eq(ClassGroup::getTenantId, TenantContext.getTenantId())
                .eq(ClassGroup::getTeacherAvailabilityId, id)
                .last("LIMIT 1"));
        if (bound != null) {
            throw new BizException(ErrorCode.CONFLICT, "该时段已绑定分组，请先换绑或解散分组后再删除");
        }
        Long futureLessons = lessonMapper.selectCount(Wrappers.<Lesson>lambdaQuery()
                .eq(Lesson::getTenantId, TenantContext.getTenantId())
                .eq(Lesson::getTeacherAvailabilityId, id)
                .ne(Lesson::getStatus, LessonStatus.CANCELLED)
                .gt(Lesson::getStartAt, LocalDateTime.now()));
        if (futureLessons != null && futureLessons > 0) {
            throw new BizException(ErrorCode.CONFLICT, "存在未来未开始的课次，禁止删除；可改为停用");
        }
        mapper.deleteById(id);
    }

    public boolean hasOverlap(Long teacherId, Integer dayOfWeek, Integer startMinute, Integer endMinute) {
        LambdaQueryWrapper<TeacherAvailability> wrapper = new LambdaQueryWrapper<TeacherAvailability>()
                .eq(TeacherAvailability::getTeacherId, teacherId)
                .eq(TeacherAvailability::getDayOfWeek, dayOfWeek)
                .eq(TeacherAvailability::getStatus, 1);
        return mapper.selectList(wrapper).stream().anyMatch(a -> overlap(a, startMinute, endMinute));
    }

    private void validateTime(TeacherAvailabilityRequest req) {
        if (req.getEndMinute() <= req.getStartMinute()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "结束时间必须晚于开始时间");
        }
    }

    private void checkOverlap(Long teacherId, Long excludeId, TeacherAvailabilityRequest req) {
        LambdaQueryWrapper<TeacherAvailability> wrapper = new LambdaQueryWrapper<TeacherAvailability>()
                .eq(TeacherAvailability::getTeacherId, teacherId)
                .eq(TeacherAvailability::getDayOfWeek, req.getDayOfWeek());
        if (excludeId != null) {
            wrapper.ne(TeacherAvailability::getId, excludeId);
        }
        List<TeacherAvailability> existing = mapper.selectList(wrapper);
        for (TeacherAvailability a : existing) {
            if (overlap(a, req.getStartMinute(), req.getEndMinute())) {
                throw new BizException(ErrorCode.CONFLICT, "时段冲突：与已有可用时段重叠");
            }
        }
    }

    private boolean overlap(TeacherAvailability existing, Integer start, Integer end) {
        return existing.getStartMinute() < end && start < existing.getEndMinute();
    }

    private List<TeacherAvailabilityResponse> toResponses(List<TeacherAvailability> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Set<Long> teacherIds = list.stream().map(TeacherAvailability::getTeacherId).collect(Collectors.toSet());
        Map<Long, String> teacherNames = userMapper.selectBatchIds(teacherIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        Set<Long> availIds = list.stream().map(TeacherAvailability::getId).collect(Collectors.toSet());
        Map<Long, Long> boundGroupByAvail = new HashMap<>();
        classGroupMapper
                .selectList(Wrappers.<ClassGroup>lambdaQuery()
                        .eq(ClassGroup::getTenantId, TenantContext.getTenantId())
                        .in(ClassGroup::getTeacherAvailabilityId, availIds))
                .forEach(g -> boundGroupByAvail.put(g.getTeacherAvailabilityId(), g.getId()));
        return list.stream()
                .map(e -> toResponse(
                        e, teacherNames.get(e.getTeacherId()), boundGroupByAvail.get(e.getId())))
                .toList();
    }

    private TeacherAvailabilityResponse toResponse(TeacherAvailability e, String teacherName, Long boundClassGroupId) {
        if (teacherName == null && e.getTeacherId() != null) {
            User t = userMapper.selectById(e.getTeacherId());
            teacherName = t != null ? t.getName() : null;
        }
        if (boundClassGroupId == null) {
            ClassGroup bound = classGroupMapper.selectOne(Wrappers.<ClassGroup>lambdaQuery()
                    .eq(ClassGroup::getTenantId, TenantContext.getTenantId())
                    .eq(ClassGroup::getTeacherAvailabilityId, e.getId())
                    .last("LIMIT 1"));
            boundClassGroupId = bound != null ? bound.getId() : null;
        }
        return TeacherAvailabilityResponse.builder()
                .id(e.getId())
                .teacherId(e.getTeacherId())
                .teacherName(teacherName)
                .branchId(e.getBranchId())
                .dayOfWeek(e.getDayOfWeek())
                .startMinute(e.getStartMinute())
                .endMinute(e.getEndMinute())
                .capacity(e.getCapacity())
                .defaultClassRoomId(e.getDefaultClassRoomId())
                .validFrom(e.getValidFrom())
                .validTo(e.getValidTo())
                .status(e.getStatus())
                .note(e.getNote())
                .boundClassGroupId(boundClassGroupId)
                .build();
    }
}
