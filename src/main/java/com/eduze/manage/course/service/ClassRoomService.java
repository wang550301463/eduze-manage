package com.eduze.manage.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.course.domain.ClassRoom;
import com.eduze.manage.course.dto.ClassRoomRequest;
import com.eduze.manage.course.dto.ClassRoomResponse;
import com.eduze.manage.course.mapper.ClassRoomMapper;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.domain.LessonStatus;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassRoomService {

    private final ClassRoomMapper classRoomMapper;
    private final LessonMapper lessonMapper;
    private final BranchAccessGuard branchAccessGuard;

    public Page<ClassRoomResponse> list(int page, int size, Long branchId) {
        if (branchId != null) {
            branchAccessGuard.requireBranchAccess(branchId);
        }
        Page<ClassRoom> result = classRoomMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<ClassRoom>lambdaQuery()
                        .eq(ClassRoom::getTenantId, TenantContext.getTenantId())
                        .eq(branchId != null, ClassRoom::getBranchId, branchId)
                        .orderByAsc(ClassRoom::getName));
        Page<ClassRoomResponse> mapped = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        mapped.setRecords(result.getRecords().stream().map(this::toResponse).toList());
        return mapped;
    }

    public ClassRoomResponse get(Long id) {
        return toResponse(requireRoom(id));
    }

    @Transactional
    public ClassRoomResponse create(ClassRoomRequest request) {
        branchAccessGuard.requireBranchAccess(request.getBranchId());
        ClassRoom room = new ClassRoom();
        room.setTenantId(TenantContext.getTenantId());
        room.setBranchId(request.getBranchId());
        room.setName(request.getName());
        room.setCapacity(request.getCapacity());
        room.setNote(request.getNote());
        classRoomMapper.insert(room);
        return toResponse(room);
    }

    @Transactional
    public ClassRoomResponse update(Long id, ClassRoomRequest request) {
        ClassRoom room = requireRoom(id);
        branchAccessGuard.requireBranchAccess(room.getBranchId());
        branchAccessGuard.requireBranchAccess(request.getBranchId());
        room.setBranchId(request.getBranchId());
        room.setName(request.getName());
        room.setCapacity(request.getCapacity());
        room.setNote(request.getNote());
        classRoomMapper.updateById(room);
        return toResponse(room);
    }

    @Transactional
    public void delete(Long id) {
        requireRoom(id);
        Long futureCount = lessonMapper.selectCount(Wrappers.<Lesson>lambdaQuery()
                .eq(Lesson::getTenantId, TenantContext.getTenantId())
                .eq(Lesson::getClassRoomId, id)
                .gt(Lesson::getStartAt, LocalDateTime.now())
                .ne(Lesson::getStatus, LessonStatus.CANCELLED));
        if (futureCount != null && futureCount > 0) {
            throw new BizException(ErrorCode.CONFLICT, "画室仍有未来课次，无法删除");
        }
        classRoomMapper.deleteById(id);
    }

    ClassRoom requireRoom(Long id) {
        ClassRoom room = classRoomMapper.selectById(id);
        if (room == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "画室不存在");
        }
        return room;
    }

    private ClassRoomResponse toResponse(ClassRoom room) {
        return ClassRoomResponse.builder()
                .id(room.getId())
                .tenantId(room.getTenantId())
                .branchId(room.getBranchId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .note(room.getNote())
                .build();
    }
}
