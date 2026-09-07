package com.eduze.manage.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.domain.Course;
import com.eduze.manage.course.dto.CourseRequest;
import com.eduze.manage.course.dto.CourseResponse;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.course.mapper.CourseMapper;
import com.eduze.manage.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final ClassGroupMapper classGroupMapper;

    public Page<CourseResponse> list(int page, int size, String keyword) {
        Page<Course> result = courseMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<Course>lambdaQuery()
                        .eq(Course::getTenantId, TenantContext.getTenantId())
                        .like(keyword != null && !keyword.isBlank(), Course::getName, keyword)
                        .orderByDesc(Course::getUpdatedAt));
        Page<CourseResponse> mapped = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        mapped.setRecords(result.getRecords().stream().map(this::toResponse).toList());
        return mapped;
    }

    public CourseResponse get(Long id) {
        Course course = requireCourse(id);
        return toResponse(course);
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        Course course = new Course();
        course.setTenantId(TenantContext.getTenantId());
        applyRequest(course, request);
        try {
            courseMapper.insert(course);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.TENANT_UNIQUE_VIOLATION, "课程名称已存在");
        }
        return toResponse(course);
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = requireCourse(id);
        applyRequest(course, request);
        try {
            courseMapper.updateById(course);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.TENANT_UNIQUE_VIOLATION, "课程名称已存在");
        }
        return toResponse(course);
    }

    @Transactional
    public void delete(Long id) {
        requireCourse(id);
        Long count = classGroupMapper.selectCount(Wrappers.<ClassGroup>lambdaQuery()
                .eq(ClassGroup::getTenantId, TenantContext.getTenantId())
                .eq(ClassGroup::getCourseId, id));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.CONFLICT, "课程下仍有班级，无法删除");
        }
        courseMapper.deleteById(id);
    }

    Course requireCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        return course;
    }

    private void applyRequest(Course course, CourseRequest request) {
        course.setName(request.getName());
        course.setAgeMin(request.getAgeMin());
        course.setAgeMax(request.getAgeMax());
        course.setLessonMinutes(request.getLessonMinutes());
        course.setCoverUrl(request.getCoverUrl());
        course.setDescription(request.getDescription());
    }

    private CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .tenantId(course.getTenantId())
                .name(course.getName())
                .ageMin(course.getAgeMin())
                .ageMax(course.getAgeMax())
                .lessonMinutes(course.getLessonMinutes())
                .coverUrl(course.getCoverUrl())
                .description(course.getDescription())
                .build();
    }
}
