package com.eduze.manage.student.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.student.domain.Guardian;
import com.eduze.manage.student.domain.StudentGuardianRelation;
import com.eduze.manage.student.dto.GuardianSummaryResponse;
import com.eduze.manage.student.mapper.GuardianMapper;
import com.eduze.manage.student.mapper.StudentGuardianRelationMapper;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentGuardianService {

    private final StudentGuardianRelationMapper relationMapper;
    private final GuardianMapper guardianMapper;

    public List<GuardianSummaryResponse> listByStudent(Long studentId, boolean pickupOnly) {
        List<StudentGuardianRelation> relations =
                relationMapper.selectList(
                        Wrappers.<StudentGuardianRelation>lambdaQuery()
                                .eq(
                                        StudentGuardianRelation::getTenantId,
                                        TenantContext.getTenantId())
                                .eq(StudentGuardianRelation::getStudentId, studentId));
        return relations.stream()
                .map(
                        rel -> {
                            Guardian g = guardianMapper.selectById(rel.getGuardianId());
                            if (g == null) {
                                return null;
                            }
                            if (pickupOnly && (g.getCanPickup() == null || g.getCanPickup() != 1)) {
                                return null;
                            }
                            return GuardianSummaryResponse.builder()
                                    .id(g.getId())
                                    .name(g.getName())
                                    .phone(g.getPhone())
                                    .relation(rel.getRelation())
                                    .canPickup(g.getCanPickup())
                                    .isMainContact(g.getIsMainContact())
                                    .build();
                        })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
