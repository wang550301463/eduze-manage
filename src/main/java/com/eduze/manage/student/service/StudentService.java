package com.eduze.manage.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.branch.domain.Branch;
import com.eduze.manage.branch.mapper.BranchMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.common.util.PhoneMask;
import com.eduze.manage.curriculum.domain.CurriculumStage;
import com.eduze.manage.curriculum.mapper.CurriculumStageMapper;
import com.eduze.manage.lesson.domain.LessonSubscription;
import com.eduze.manage.lesson.mapper.LessonSubscriptionMapper;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import com.eduze.manage.teacher.mapper.TeacherAvailabilityMapper;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.dto.StudentRequest;
import com.eduze.manage.student.dto.StudentResponse;
import com.eduze.manage.student.dto.StudentStatusRequest;
import com.eduze.manage.student.dto.StudentUpdateRequest;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.manage.tenant.TenantContext;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentMapper studentMapper;
    private final BranchMapper branchMapper;
    private final PackageBalanceHelper packageBalanceHelper;
    private final JdbcTemplate jdbcTemplate;
    private final StudentMentorService mentorService;
    private final UserMapper userMapper;
    private final CurriculumStageMapper stageMapper;
    private final LessonSubscriptionMapper subscriptionMapper;
    private final TeacherAvailabilityMapper availabilityMapper;
    private final BranchAccessGuard branchAccessGuard;

    public Page<StudentResponse> list(
            String keyword,
            Long branchId,
            Long classGroupId,
            Integer status,
            Integer pkgRemainingMax,
            boolean maskPhone,
            int page,
            int size) {
        if (classGroupId != null) {
            Page<StudentResponse> empty = new Page<>(page, size, 0);
            empty.setRecords(List.of());
            return empty;
        }

        LambdaQueryWrapper<Student> wrapper = Wrappers.<Student>lambdaQuery()
                .eq(Student::getTenantId, TenantContext.getTenantId())
                .orderByDesc(Student::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Student::getName, keyword).or().like(Student::getEnrollNo, keyword));
        }
        if (branchId != null) {
            branchAccessGuard.requireBranchAccess(branchId);
            wrapper.eq(Student::getBranchId, branchId);
        }
        if (status != null) {
            wrapper.eq(Student::getStatus, status);
        }
        if (pkgRemainingMax != null) {
            List<Long> ids = findStudentIdsByMaxRemaining(pkgRemainingMax);
            if (ids.isEmpty()) {
                Page<StudentResponse> empty = new Page<>(page, size, 0);
                empty.setRecords(List.of());
                return empty;
            }
            wrapper.in(Student::getId, ids);
        }

        Page<Student> result = studentMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, String> branchNames = loadBranchNames(result.getRecords());
        Map<Long, Integer> balances = packageBalanceHelper.sumRemaining(
                result.getRecords().stream().map(Student::getId).toList());

        Page<StudentResponse> mapped = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        mapped.setRecords(result.getRecords().stream()
                .map(s -> toResponse(s, branchNames, balances, maskPhone))
                .toList());
        return mapped;
    }

    public StudentResponse get(Long id, boolean maskPhone) {
        Student student = requireStudent(id);
        branchAccessGuard.requireBranchAccess(student.getBranchId());
        Map<Long, String> branchNames = loadBranchNames(List.of(student));
        Map<Long, Integer> balances = packageBalanceHelper.sumRemaining(List.of(id));
        return toResponse(student, branchNames, balances, maskPhone);
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        branchAccessGuard.requireBranchAccess(request.getBranchId());
        mentorService.requireMentor(request.getMentorTeacherId(), request.getBranchId());
        Student student = new Student();
        student.setTenantId(TenantContext.getTenantId());
        applyRequest(student, request);
        try {
            studentMapper.insert(student);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.TENANT_UNIQUE_VIOLATION, "入园编号已存在");
        }
        mentorService.recordHistory(
                student.getId(), student.getBranchId(), null, student.getMentorTeacherId(), "首次绑定");
        if (request.getInitialSubscriptions() != null) {
            for (StudentRequest.InitialSubscription sub : request.getInitialSubscriptions()) {
                createInitialSubscription(student, sub);
            }
        }
        return get(student.getId(), false);
    }

    private void createInitialSubscription(Student student, StudentRequest.InitialSubscription sub) {
        TeacherAvailability avail = availabilityMapper.selectById(sub.getTeacherAvailabilityId());
        if (avail == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "可用时段不存在");
        }
        if (!avail.getBranchId().equals(student.getBranchId())) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "可用时段不属于该校区");
        }
        LessonSubscription subEntity = new LessonSubscription();
        subEntity.setTenantId(TenantContext.getTenantId());
        subEntity.setBranchId(student.getBranchId());
        subEntity.setStudentId(student.getId());
        subEntity.setTeacherId(avail.getTeacherId());
        subEntity.setTeacherAvailabilityId(sub.getTeacherAvailabilityId());
        subEntity.setValidFrom(sub.getValidFrom());
        subEntity.setValidTo(sub.getValidTo());
        subEntity.setStatus(1);
        subEntity.setSource("NORMAL");
        try {
            subscriptionMapper.insert(subEntity);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "已存在相同订阅");
        }
    }

    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest request) {
        if (!id.equals(request.getId())) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "路径 ID 与请求体不一致");
        }
        Student student = requireStudent(id);
        applyUpdate(student, request);
        try {
            studentMapper.updateById(student);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.TENANT_UNIQUE_VIOLATION, "入园编号已存在");
        }
        return get(id, false);
    }

    @Transactional
    public StudentResponse updateStatus(Long id, StudentStatusRequest request) {
        Student student = requireStudent(id);
        student.setStatus(request.getStatus());
        studentMapper.updateById(student);
        return get(id, false);
    }

    @Transactional
    public void delete(Long id) {
        Student student = requireStudent(id);
        studentMapper.deleteById(student.getId());
    }

    public Student requireStudent(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        }
        return student;
    }

    private List<Long> findStudentIdsByMaxRemaining(int maxRemaining) {
        Long tenantId = TenantContext.getTenantId();
        return jdbcTemplate.queryForList(
                """
                SELECT student_id FROM t_course_package
                WHERE tenant_id = ? AND deleted_at = 0
                  AND (expire_date IS NULL OR expire_date >= CURDATE())
                GROUP BY student_id
                HAVING COALESCE(SUM(remaining_lessons), 0) <= ?
                """,
                Long.class,
                tenantId,
                maxRemaining);
    }

    private Map<Long, String> loadBranchNames(List<Student> students) {
        Set<Long> branchIds =
                students.stream().map(Student::getBranchId).collect(Collectors.toSet());
        if (branchIds.isEmpty()) {
            return Map.of();
        }
        List<Branch> branches = branchMapper.selectBatchIds(branchIds);
        return branches.stream().collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));
    }

    private void applyRequest(Student student, StudentRequest request) {
        student.setBranchId(request.getBranchId());
        student.setEnrollNo(request.getEnrollNo().trim());
        student.setName(request.getName().trim());
        student.setGender(request.getGender() != null ? request.getGender() : 0);
        student.setBirthday(request.getBirthday());
        student.setEnrollDate(request.getEnrollDate());
        student.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        student.setAllergy(request.getAllergy());
        student.setHealthNote(request.getHealthNote());
        student.setEmergencyContact(request.getEmergencyContact());
        student.setEmergencyPhone(request.getEmergencyPhone());
        student.setAvatarUrl(request.getAvatarUrl());
        student.setMentorTeacherId(request.getMentorTeacherId());
        student.setCurrentStageId(request.getCurrentStageId());
    }

    private void applyUpdate(Student student, StudentUpdateRequest request) {
        student.setBranchId(request.getBranchId());
        student.setEnrollNo(request.getEnrollNo().trim());
        student.setName(request.getName().trim());
        student.setGender(request.getGender() != null ? request.getGender() : 0);
        student.setBirthday(request.getBirthday());
        student.setEnrollDate(request.getEnrollDate());
        if (request.getStatus() != null) {
            student.setStatus(request.getStatus());
        }
        student.setAllergy(request.getAllergy());
        student.setHealthNote(request.getHealthNote());
        student.setEmergencyContact(request.getEmergencyContact());
        student.setEmergencyPhone(request.getEmergencyPhone());
        student.setAvatarUrl(request.getAvatarUrl());
        student.setCurrentStageId(request.getCurrentStageId());
    }

    private StudentResponse toResponse(
            Student student,
            Map<Long, String> branchNames,
            Map<Long, Integer> balances,
            boolean maskPhone) {
        int totalRemaining = balances.getOrDefault(student.getId(), 0);
        String phone = student.getEmergencyPhone();
        if (maskPhone && phone != null) {
            phone = PhoneMask.mask(phone);
        }
        String mentorName = null;
        if (student.getMentorTeacherId() != null) {
            User mentor = userMapper.selectById(student.getMentorTeacherId());
            if (mentor != null) {
                mentorName = mentor.getName();
            }
        }
        String stageCode = null;
        String stageName = null;
        if (student.getCurrentStageId() != null) {
            CurriculumStage stage = stageMapper.selectById(student.getCurrentStageId());
            if (stage != null) {
                stageCode = stage.getCode();
                stageName = stage.getName();
            }
        }
        return StudentResponse.builder()
                .id(student.getId())
                .tenantId(student.getTenantId())
                .branchId(student.getBranchId())
                .branchName(branchNames.get(student.getBranchId()))
                .enrollNo(student.getEnrollNo())
                .name(student.getName())
                .gender(student.getGender())
                .birthday(student.getBirthday())
                .enrollDate(student.getEnrollDate())
                .status(student.getStatus())
                .allergy(student.getAllergy())
                .healthNote(student.getHealthNote())
                .emergencyContact(student.getEmergencyContact())
                .emergencyPhone(phone)
                .avatarUrl(student.getAvatarUrl())
                .mentorTeacherId(student.getMentorTeacherId())
                .mentorTeacherName(mentorName)
                .currentStageId(student.getCurrentStageId())
                .currentStageCode(stageCode)
                .currentStageName(stageName)
                .classGroups(Collections.emptyList())
                .totalRemaining(totalRemaining)
                .alertLow(packageBalanceHelper.isAlertLow(totalRemaining))
                .build();
    }
}
