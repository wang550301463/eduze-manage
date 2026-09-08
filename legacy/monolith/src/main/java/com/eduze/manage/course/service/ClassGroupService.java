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
import com.eduze.manage.course.dto.NestedTeacherAvailabilityRequest;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.course.mapper.CourseMapper;
import com.eduze.manage.course.mapper.StudentClassGroupMapper;
import com.eduze.manage.lesson.domain.LessonSubscription;
import com.eduze.manage.lesson.mapper.LessonSubscriptionMapper;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.dto.AssignMentorRequest;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.student.service.StudentMentorService;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import com.eduze.manage.teacher.dto.TeacherAvailabilityRequest;
import com.eduze.manage.teacher.mapper.TeacherAvailabilityMapper;
import com.eduze.manage.teacher.service.TeacherAvailabilityService;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassGroupService {

    private final ClassGroupMapper classGroupMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final StudentClassGroupMapper studentClassGroupMapper;
    private final TeacherAvailabilityMapper teacherAvailabilityMapper;
    private final TeacherAvailabilityService teacherAvailabilityService;
    private final LessonSubscriptionMapper lessonSubscriptionMapper;
    private final StudentMapper studentMapper;
    private final StudentMentorService studentMentorService;
    private final BranchAccessGuard branchAccessGuard;

    public Page<ClassGroupResponse> list(int page, int size, Long branchId, Long courseId) {
        if (branchId != null) {
            branchAccessGuard.requireBranchAccess(branchId);
        }
        Page<ClassGroup> result =
                classGroupMapper.selectPage(
                        new Page<>(page, size),
                        Wrappers.<ClassGroup>lambdaQuery()
                                .eq(ClassGroup::getTenantId, TenantContext.getTenantId())
                                .eq(branchId != null, ClassGroup::getBranchId, branchId)
                                .eq(courseId != null, ClassGroup::getCourseId, courseId)
                                .orderByDesc(ClassGroup::getUpdatedAt));
        Page<ClassGroupResponse> mapped =
                new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        mapped.setRecords(result.getRecords().stream().map(g -> toResponse(g, null)).toList());
        return mapped;
    }

    public ClassGroupResponse get(Long id) {
        return toResponse(requireGroup(id), null);
    }

    @Transactional
    public ClassGroupResponse create(ClassGroupRequest request) {
        branchAccessGuard.requireBranchAccess(request.getBranchId());
        if (request.getCourseId() != null) {
            requireCourseExists(request.getCourseId());
        }
        Long availId = resolveAvailabilityIdForCreate(request);
        TeacherAvailability avail = requireBindableAvailability(availId, request.getBranchId());

        ClassGroup group = new ClassGroup();
        group.setTenantId(TenantContext.getTenantId());
        group.setBranchId(request.getBranchId());
        group.setName(request.getName());
        group.setCourseId(request.getCourseId());
        group.setHeadTeacherId(avail.getTeacherId());
        group.setTeacherAvailabilityId(avail.getId());
        group.setCapacity(avail.getCapacity());
        group.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        group.setTagColor(request.getTagColor());
        try {
            classGroupMapper.insert(group);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "该可用时段已被其他分组绑定", ex);
        }
        return toResponse(group, null);
    }

    @Transactional
    public ClassGroupResponse update(Long id, ClassGroupRequest request) {
        ClassGroup group = requireGroup(id);
        branchAccessGuard.requireBranchAccess(group.getBranchId());
        branchAccessGuard.requireBranchAccess(request.getBranchId());
        if (request.getCourseId() != null) {
            requireCourseExists(request.getCourseId());
        }

        Long newAvailId = request.getTeacherAvailabilityId();
        if (request.getNestedAvailability() != null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "更新分组请绑定已有时段，不支持现场新建");
        }

        String message = null;
        Long oldAvailId = group.getTeacherAvailabilityId();
        if (newAvailId != null && !Objects.equals(newAvailId, oldAvailId)) {
            TeacherAvailability newAvail =
                    requireBindableAvailability(newAvailId, request.getBranchId());
            migrateSubscriptionsAndMentors(group, oldAvailId, newAvail);
            group.setTeacherAvailabilityId(newAvail.getId());
            group.setHeadTeacherId(newAvail.getTeacherId());
            group.setCapacity(newAvail.getCapacity());
            message = "时段已换绑，请按需重新批量生成未来课次";
        } else if (oldAvailId != null) {
            TeacherAvailability avail = teacherAvailabilityMapper.selectById(oldAvailId);
            if (avail != null) {
                group.setHeadTeacherId(avail.getTeacherId());
                group.setCapacity(avail.getCapacity());
            }
        } else if (request.getHeadTeacherId() != null) {
            group.setHeadTeacherId(request.getHeadTeacherId());
        }
        if (newAvailId == null && oldAvailId == null && request.getCapacity() != null) {
            group.setCapacity(request.getCapacity());
        }

        group.setBranchId(request.getBranchId());
        group.setName(request.getName());
        group.setCourseId(request.getCourseId());
        if (request.getStatus() != null) {
            group.setStatus(request.getStatus());
        }
        group.setTagColor(request.getTagColor());
        try {
            classGroupMapper.updateById(group);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "该可用时段已被其他分组绑定", ex);
        }
        return toResponse(group, message);
    }

    @Transactional
    public void delete(Long id) {
        requireGroup(id);
        Long activeMembers =
                studentClassGroupMapper.selectCount(
                        Wrappers.<StudentClassGroup>lambdaQuery()
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

    public ClassGroup findByAvailabilityId(Long teacherAvailabilityId) {
        if (teacherAvailabilityId == null) {
            return null;
        }
        return classGroupMapper.selectOne(
                Wrappers.<ClassGroup>lambdaQuery()
                        .eq(ClassGroup::getTenantId, TenantContext.getTenantId())
                        .eq(ClassGroup::getTeacherAvailabilityId, teacherAvailabilityId)
                        .last("LIMIT 1"));
    }

    int countActiveMembers(Long classGroupId) {
        Long count =
                studentClassGroupMapper.selectCount(
                        Wrappers.<StudentClassGroup>lambdaQuery()
                                .eq(StudentClassGroup::getTenantId, TenantContext.getTenantId())
                                .eq(StudentClassGroup::getClassGroupId, classGroupId)
                                .isNull(StudentClassGroup::getLeftAt));
        return count != null ? count.intValue() : 0;
    }

    private Long resolveAvailabilityIdForCreate(ClassGroupRequest request) {
        if (request.getNestedAvailability() != null) {
            NestedTeacherAvailabilityRequest nested = request.getNestedAvailability();
            if (request.getTeacherAvailabilityId() != null) {
                throw new BizException(
                        ErrorCode.VALIDATION_FAILED,
                        "teacherAvailabilityId 与 nestedAvailability 只能二选一");
            }
            if (!Objects.equals(nested.getBranchId(), request.getBranchId())) {
                throw new BizException(ErrorCode.VALIDATION_FAILED, "新建时段校区须与分组校区一致");
            }
            TeacherAvailabilityRequest availReq = new TeacherAvailabilityRequest();
            availReq.setBranchId(nested.getBranchId());
            availReq.setDayOfWeek(nested.getDayOfWeek());
            availReq.setStartMinute(nested.getStartMinute());
            availReq.setEndMinute(nested.getEndMinute());
            availReq.setCapacity(nested.getCapacity());
            availReq.setDefaultClassRoomId(nested.getDefaultClassRoomId());
            availReq.setValidFrom(nested.getValidFrom());
            availReq.setValidTo(nested.getValidTo());
            availReq.setStatus(nested.getStatus() != null ? nested.getStatus() : 1);
            availReq.setNote(nested.getNote());
            return teacherAvailabilityService.create(nested.getTeacherId(), availReq).getId();
        }
        if (request.getTeacherAvailabilityId() == null) {
            throw new BizException(
                    ErrorCode.VALIDATION_FAILED,
                    "新建分组须绑定老师可用时段（teacherAvailabilityId 或 nestedAvailability）");
        }
        return request.getTeacherAvailabilityId();
    }

    private TeacherAvailability requireBindableAvailability(Long availId, Long branchId) {
        TeacherAvailability avail = teacherAvailabilityMapper.selectById(availId);
        if (avail == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "可用时段不存在");
        }
        if (avail.getStatus() == null || avail.getStatus() != 1) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "可用时段未启用，无法绑定");
        }
        if (!Objects.equals(avail.getBranchId(), branchId)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "可用时段与分组不在同一校区");
        }
        ClassGroup bound = findByAvailabilityId(availId);
        if (bound != null) {
            throw new BizException(ErrorCode.CONFLICT, "该可用时段已被其他分组绑定");
        }
        return avail;
    }

    private void migrateSubscriptionsAndMentors(
            ClassGroup group, Long oldAvailId, TeacherAvailability newAvail) {
        List<StudentClassGroup> members =
                studentClassGroupMapper.selectList(
                        Wrappers.<StudentClassGroup>lambdaQuery()
                                .eq(StudentClassGroup::getTenantId, TenantContext.getTenantId())
                                .eq(StudentClassGroup::getClassGroupId, group.getId())
                                .isNull(StudentClassGroup::getLeftAt));
        LocalDate today = LocalDate.now();
        for (StudentClassGroup membership : members) {
            Long studentId = membership.getStudentId();
            if (oldAvailId != null) {
                List<LessonSubscription> oldSubs =
                        lessonSubscriptionMapper.selectList(
                                Wrappers.<LessonSubscription>lambdaQuery()
                                        .eq(
                                                LessonSubscription::getTenantId,
                                                TenantContext.getTenantId())
                                        .eq(LessonSubscription::getStudentId, studentId)
                                        .eq(
                                                LessonSubscription::getTeacherAvailabilityId,
                                                oldAvailId)
                                        .eq(LessonSubscription::getStatus, 1));
                for (LessonSubscription sub : oldSubs) {
                    sub.setTeacherAvailabilityId(newAvail.getId());
                    sub.setTeacherId(newAvail.getTeacherId());
                    lessonSubscriptionMapper.updateById(sub);
                }
            }
            Student student = studentMapper.selectById(studentId);
            if (student != null
                    && !Objects.equals(student.getMentorTeacherId(), newAvail.getTeacherId())) {
                AssignMentorRequest mentorReq = new AssignMentorRequest();
                mentorReq.setToTeacherId(newAvail.getTeacherId());
                mentorReq.setReason("换绑时段同步主带");
                mentorReq.setKeepSubscriptions(true);
                studentMentorService.changeMentor(studentId, mentorReq);
            }
            ensureActiveSubscription(studentId, group.getBranchId(), newAvail, today);
        }
    }

    private void ensureActiveSubscription(
            Long studentId, Long branchId, TeacherAvailability avail, LocalDate today) {
        Long existing =
                lessonSubscriptionMapper.selectCount(
                        Wrappers.<LessonSubscription>lambdaQuery()
                                .eq(LessonSubscription::getTenantId, TenantContext.getTenantId())
                                .eq(LessonSubscription::getStudentId, studentId)
                                .eq(LessonSubscription::getTeacherAvailabilityId, avail.getId())
                                .eq(LessonSubscription::getStatus, 1));
        if (existing != null && existing > 0) {
            return;
        }
        LessonSubscription sub = new LessonSubscription();
        sub.setTenantId(TenantContext.getTenantId());
        sub.setBranchId(branchId);
        sub.setStudentId(studentId);
        sub.setTeacherId(avail.getTeacherId());
        sub.setTeacherAvailabilityId(avail.getId());
        sub.setValidFrom(today);
        sub.setStatus(1);
        sub.setSource("NORMAL");
        lessonSubscriptionMapper.insert(sub);
    }

    private void requireCourseExists(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
    }

    ClassGroupResponse toResponse(ClassGroup group, String message) {
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
        Integer dayOfWeek = null;
        Integer startMinute = null;
        Integer endMinute = null;
        if (group.getTeacherAvailabilityId() != null) {
            TeacherAvailability avail =
                    teacherAvailabilityMapper.selectById(group.getTeacherAvailabilityId());
            if (avail != null) {
                dayOfWeek = avail.getDayOfWeek();
                startMinute = avail.getStartMinute();
                endMinute = avail.getEndMinute();
                if (teacherName == null && avail.getTeacherId() != null) {
                    User t = userMapper.selectById(avail.getTeacherId());
                    teacherName = t != null ? t.getName() : null;
                }
            }
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
                .teacherAvailabilityId(group.getTeacherAvailabilityId())
                .dayOfWeek(dayOfWeek)
                .startMinute(startMinute)
                .endMinute(endMinute)
                .teacherName(teacherName)
                .message(message)
                .build();
    }
}
