package com.eduze.manage.student.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.student.domain.CoursePackage;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.dto.CoursePackageRequest;
import com.eduze.manage.student.dto.CoursePackageResponse;
import com.eduze.manage.student.mapper.CoursePackageMapper;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoursePackageService {

    private final CoursePackageMapper coursePackageMapper;
    private final StudentService studentService;
    private final PackageBalanceHelper packageBalanceHelper;

    public List<CoursePackageResponse> listByStudent(Long studentId) {
        Student student = studentService.requireStudent(studentId);
        List<CoursePackage> packages =
                coursePackageMapper.selectList(
                        Wrappers.<CoursePackage>lambdaQuery()
                                .eq(CoursePackage::getStudentId, studentId)
                                .orderByDesc(CoursePackage::getId));
        return packages.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CoursePackageResponse create(Long studentId, CoursePackageRequest request) {
        Student student = studentService.requireStudent(studentId);
        CoursePackage pkg = new CoursePackage();
        pkg.setTenantId(student.getTenantId());
        pkg.setBranchId(student.getBranchId());
        pkg.setStudentId(studentId);
        if (request.getRemainingLessons() > request.getTotalLessons()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "剩余课时不能超过总课时");
        }
        pkg.setCourseId(request.getCourseId());
        pkg.setTotalLessons(request.getTotalLessons());
        pkg.setRemainingLessons(request.getRemainingLessons());
        pkg.setExpireDate(request.getExpireDate());
        pkg.setNote(request.getNote());
        coursePackageMapper.insert(pkg);
        return toResponse(pkg);
    }

    @Transactional
    public CoursePackageResponse update(Long id, CoursePackageRequest request) {
        CoursePackage pkg = requirePackage(id);
        if (pkg.getSourceOrderId() != null
                || (pkg.getFrozenLessons() != null && pkg.getFrozenLessons() > 0)) {
            throw new BizException(ErrorCode.CONFLICT, "商城课时包需通过订单流程调整");
        }
        if (request.getRemainingLessons() > request.getTotalLessons()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "剩余课时不能超过总课时");
        }
        pkg.setCourseId(request.getCourseId());
        pkg.setTotalLessons(request.getTotalLessons());
        pkg.setRemainingLessons(request.getRemainingLessons());
        pkg.setExpireDate(request.getExpireDate());
        pkg.setNote(request.getNote());
        coursePackageMapper.updateById(pkg);
        return toResponse(pkg);
    }

    @Transactional
    public void delete(Long id) {
        CoursePackage pkg = requirePackage(id);
        if (pkg.getSourceOrderId() != null
                || (pkg.getFrozenLessons() != null && pkg.getFrozenLessons() > 0)) {
            throw new BizException(ErrorCode.CONFLICT, "商城课时包不可直接删除");
        }
        coursePackageMapper.deleteById(id);
    }

    private CoursePackage requirePackage(Long id) {
        CoursePackage pkg = coursePackageMapper.selectById(id);
        if (pkg == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课时包不存在");
        }
        return pkg;
    }

    private CoursePackageResponse toResponse(CoursePackage pkg) {
        boolean expired =
                pkg.getExpireDate() != null && pkg.getExpireDate().isBefore(LocalDate.now());
        int frozen = pkg.getFrozenLessons() == null ? 0 : pkg.getFrozenLessons();
        int remaining =
                expired
                        ? 0
                        : (pkg.getRemainingLessons() != null
                                ? pkg.getRemainingLessons() - frozen
                                : 0);
        return CoursePackageResponse.builder()
                .id(pkg.getId())
                .studentId(pkg.getStudentId())
                .branchId(pkg.getBranchId())
                .totalLessons(pkg.getTotalLessons())
                .remainingLessons(remaining)
                .frozenLessons(frozen)
                .courseId(pkg.getCourseId())
                .expireDate(pkg.getExpireDate())
                .note(pkg.getNote())
                .alertLow(!expired && packageBalanceHelper.isAlertLow(remaining))
                .build();
    }
}
