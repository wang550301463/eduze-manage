package com.eduze.manage.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.domain.Course;
import com.eduze.manage.course.domain.StudentClassGroup;
import com.eduze.manage.course.dto.ClassGroupRequest;
import com.eduze.manage.course.dto.ClassGroupResponse;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.course.mapper.CourseMapper;
import com.eduze.manage.course.mapper.StudentClassGroupMapper;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.manage.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassGroupService {

    private final ClassGroupMapper classGroupMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final StudentClassGroupMapper studentClassGroupMapper;
    private final BranchAccessGuard branchAccessGuard;

    public Page<ClassGroupResponse> list(int page, int size, Long branchId, Long courseId) {
        if (branchId != null) {
            branchAccessGuard.requireBranchAccess(branchId);
        }
        Page<ClassGroup> result = classGroupMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<ClassGroup>lambdaQuery()
                        .eq(ClassGroup::getTenantId, TenantContext.getTenantId())
                        .eq(branchId != null, ClassGroup::getBranchId, branchId)
                        .eq(courseId != null, ClassGroup::getCourseId, courseId)
                        .orderByDesc(ClassGroup::getUpdatedAt));
        Page<ClassGroupResponse> mapped = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        mapped.setRecords(result.getRecords().stream().map(this::toResponse).toList());
        return mapped;
    }

    public ClassGroupResponse get(Long id) {
        return toResponse(requireGroup(id));
    }

    @Transactional
    public ClassGroupResponse create(ClassGroupRequest request) {
        branchAccessGuard.requireBranchAccess(request.getBranchId());
        if (request.getCourseId() != null) {
            requireCourseExists(request.getCourseId());
        }
        ClassGroup group = new ClassGroup();
        group.setTenantId(TenantContext.getTenantId());
        group.setBranchId(request.getBranchId());
        group.setName(request.getName());
        group.setCourseId(request.getCourseId());
        group.setHeadTeacherId(request.getHeadTeacherId());
        group.setCapacity(request.getCapacity());
        group.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        group.setTagColor(request.getTagColor());
        classGroupMapper.insert(group);
        return toResponse(group);
    }

    @Transactional
    public ClassGroupResponse update(Long id, ClassGroupRequest request) {
        ClassGroup group = requireGroup(id);
        branchAccessGuard.requireBranchAccess(group.getBranchId());
        branchAccessGuard.requireBranchAccess(request.getBranchId());
        if (request.getCourseId() != null) {
            requireCourseExists(request.getCourseId());
        }
        group.setBranchId(request.getBranchId());
        group.setName(request.getName());
        group.setCourseId(request.getCourseId());
        group.setHeadTeacherId(request.getHeadTeacherId());
        group.setCapacity(request.getCapacity());
        if (request.getStatus() != null) {
            group.setStatus(request.getStatus());
        }
        group.setTagColor(request.getTagColor());
        classGroupMapper.updateById(group);
        return toResponse(group);
    }

    @Transactional
    public void delete(Long id) {
        requireGroup(id);
        Long activeMembers = studentClassGroupMapper.selectCount(Wrappers.<StudentClassGroup>lambdaQuery()
                .eq(StudentClassGroup::getTenantId, TenantContext.getTenantId())
                .eq(StudentClassGroup::getClassGroupId, id)
                .isNull(StudentClassGroup::getLeftAt));
        if (activeMembers != null && activeMembers > 0) {
            throw new BizException(ErrorCode.CONFLICT, "班级仍有在读成员，无法删除");
        }
        classGroupMapper.deleteById(id);
    }

    public ClassGroup requireGroup(Long id) {
        ClassGroup group = classGroupMapper.selectById(id);
        if (group == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "班级不存在");
        }
        return group;
    }

    int countActiveMembers(Long classGroupId) {
        Long count = studentClassGroupMapper.selectCount(Wrappers.<StudentClassGroup>lambdaQuery()
                .eq(StudentClassGroup::getTenantId, TenantContext.getTenantId())
                .eq(StudentClassGroup::getClassGroupId, classGroupId)
                .isNull(StudentClassGroup::getLeftAt));
        return count != null ? count.intValue() : 0;
    }

    private void requireCourseExists(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
    }

    ClassGroupResponse toResponse(ClassGroup group) {
        String courseName = null;
        if (group.getCourseId() != null) {
            Course course = courseMapper.selectById(group.getCourseId());
            courseName = course != null ? course.getName() : null;
        }
        String teacherName = null;
        if (group.getHeadTeacherId() != null) {
            User teacher = userMapper.selectById(group.getHeadTeacherId());
            teacherName = teacher != null ? teacher.getName() : null;
        }
        return ClassGroupResponse.builder()
                .id(group.getId())
                .tenantId(group.getTenantId())
                .branchId(group.getBranchId())
                .name(group.getName())
                .courseId(group.getCourseId())
                .courseName(courseName)
                .headTeacherId(group.getHeadTeacherId())
                .headTeacherName(teacherName)
                .capacity(group.getCapacity())
                .currentCount(countActiveMembers(group.getId()))
                .status(group.getStatus())
                .tagColor(group.getTagColor())
                .build();
    }
}
