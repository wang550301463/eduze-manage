package com.eduze.manage.student.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.student.domain.CoursePackage;
import com.eduze.manage.student.mapper.CoursePackageMapper;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PackageBalanceHelper {

    private final CoursePackageMapper coursePackageMapper;

    public int totalRemaining(Long studentId) {
        return sumRemaining(List.of(studentId)).getOrDefault(studentId, 0);
    }

    public Map<Long, Integer> sumRemaining(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Map.of();
        }
        LocalDate today = LocalDate.now();
        List<CoursePackage> packages = coursePackageMapper.selectList(
                Wrappers.<CoursePackage>lambdaQuery().in(CoursePackage::getStudentId, studentIds));
        Map<Long, Integer> sums = new HashMap<>();
        for (CoursePackage pkg : packages) {
            if (pkg.getExpireDate() != null && pkg.getExpireDate().isBefore(today)) {
                continue;
            }
            int remaining = pkg.getRemainingLessons() != null ? pkg.getRemainingLessons() : 0;
            sums.merge(pkg.getStudentId(), remaining, Integer::sum);
        }
        return sums;
    }

    public boolean isAlertLow(int totalRemaining) {
        return totalRemaining <= 5;
    }
}
