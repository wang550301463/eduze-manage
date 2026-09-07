package com.eduze.manage.teacher.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import com.eduze.manage.teacher.dto.TeacherAvailabilityRequest;
import com.eduze.manage.teacher.dto.TeacherAvailabilityResponse;
import com.eduze.manage.teacher.mapper.TeacherAvailabilityMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherAvailabilityService {

    private final TeacherAvailabilityMapper mapper;

    public List<TeacherAvailabilityResponse> list(Long teacherId) {
        LambdaQueryWrapper<TeacherAvailability> wrapper = new LambdaQueryWrapper<TeacherAvailability>()
                .eq(TeacherAvailability::getTeacherId, teacherId)
                .orderByAsc(TeacherAvailability::getDayOfWeek)
                .orderByAsc(TeacherAvailability::getStartMinute);
        return mapper.selectList(wrapper).stream().map(this::toResponse).toList();
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
        return toResponse(entity);
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
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (mapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "可用时段不存在");
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

    private TeacherAvailabilityResponse toResponse(TeacherAvailability e) {
        return TeacherAvailabilityResponse.builder()
                .id(e.getId())
                .teacherId(e.getTeacherId())
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
                .build();
    }
}
