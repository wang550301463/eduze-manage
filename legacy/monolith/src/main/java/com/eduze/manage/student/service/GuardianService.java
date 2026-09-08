package com.eduze.manage.student.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.student.domain.Guardian;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.domain.StudentGuardianRelation;
import com.eduze.manage.student.dto.GuardianQrResponse;
import com.eduze.manage.student.dto.GuardianRequest;
import com.eduze.manage.student.dto.GuardianResponse;
import com.eduze.manage.student.dto.GuardianUpsertRequest;
import com.eduze.manage.student.dto.LinkGuardianRequest;
import com.eduze.manage.student.mapper.GuardianMapper;
import com.eduze.manage.student.mapper.StudentGuardianRelationMapper;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private final GuardianMapper guardianMapper;
    private final StudentGuardianRelationMapper relationMapper;
    private final StudentMapper studentMapper;

    @Transactional
    public GuardianResponse create(GuardianRequest request) {
        Guardian guardian = new Guardian();
        guardian.setTenantId(TenantContext.getTenantId());
        guardian.setName(request.getName());
        guardian.setPhone(request.getPhone());
        guardian.setIsMainContact(
                request.getIsMainContact() != null ? request.getIsMainContact() : 0);
        guardian.setCanPickup(request.getCanPickup() != null ? request.getCanPickup() : 1);
        guardian.setQrCode(request.getQrCode());
        guardianMapper.insert(guardian);
        return toResponse(guardian, null);
    }

    @Transactional
    public GuardianResponse linkToStudent(
            Long studentId, Long guardianId, LinkGuardianRequest request) {
        requireStudent(studentId);
        Guardian guardian = requireGuardian(guardianId);

        StudentGuardianRelation existing =
                relationMapper.selectOne(
                        Wrappers.<StudentGuardianRelation>lambdaQuery()
                                .eq(
                                        StudentGuardianRelation::getTenantId,
                                        TenantContext.getTenantId())
                                .eq(StudentGuardianRelation::getStudentId, studentId)
                                .eq(StudentGuardianRelation::getGuardianId, guardianId));
        if (existing == null) {
            StudentGuardianRelation relation = new StudentGuardianRelation();
            relation.setTenantId(TenantContext.getTenantId());
            relation.setStudentId(studentId);
            relation.setGuardianId(guardianId);
            relation.setRelation(request.getRelation());
            relationMapper.insert(relation);
        } else {
            existing.setRelation(request.getRelation());
            relationMapper.updateById(existing);
        }
        return toResponse(guardian, request.getRelation());
    }

    @Transactional
    public GuardianQrResponse generateQr(Long guardianId) {
        Guardian guardian = guardianMapper.selectById(guardianId);
        if (guardian == null || !TenantContext.getTenantId().equals(guardian.getTenantId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "家长不存在");
        }
        String qrCode = UUID.randomUUID().toString().replace("-", "");
        guardian.setQrCode(qrCode);
        guardianMapper.updateById(guardian);
        return GuardianQrResponse.builder().guardianId(guardianId).qrCode(qrCode).build();
    }

    @Transactional
    public GuardianResponse upsertForStudent(Long studentId, GuardianUpsertRequest request) {
        requireStudent(studentId);
        Guardian guardian =
                guardianMapper.selectOne(
                        Wrappers.<Guardian>lambdaQuery()
                                .eq(Guardian::getTenantId, TenantContext.getTenantId())
                                .eq(Guardian::getPhone, request.getPhone()));
        if (guardian == null) {
            guardian = new Guardian();
            guardian.setTenantId(TenantContext.getTenantId());
            guardian.setName(request.getName());
            guardian.setPhone(request.getPhone());
            guardian.setIsMainContact(
                    request.getIsMainContact() != null ? request.getIsMainContact() : 0);
            guardian.setCanPickup(request.getCanPickup() != null ? request.getCanPickup() : 1);
            guardianMapper.insert(guardian);
        } else {
            guardian.setName(request.getName());
            if (request.getIsMainContact() != null) {
                guardian.setIsMainContact(request.getIsMainContact());
            }
            if (request.getCanPickup() != null) {
                guardian.setCanPickup(request.getCanPickup());
            }
            guardianMapper.updateById(guardian);
        }

        StudentGuardianRelation existing =
                relationMapper.selectOne(
                        Wrappers.<StudentGuardianRelation>lambdaQuery()
                                .eq(
                                        StudentGuardianRelation::getTenantId,
                                        TenantContext.getTenantId())
                                .eq(StudentGuardianRelation::getStudentId, studentId)
                                .eq(StudentGuardianRelation::getGuardianId, guardian.getId()));
        if (existing == null) {
            StudentGuardianRelation relation = new StudentGuardianRelation();
            relation.setTenantId(TenantContext.getTenantId());
            relation.setStudentId(studentId);
            relation.setGuardianId(guardian.getId());
            relation.setRelation(request.getRelation());
            relationMapper.insert(relation);
        } else {
            existing.setRelation(request.getRelation());
            relationMapper.updateById(existing);
        }

        if (Objects.equals(guardian.getIsMainContact(), 1)) {
            enforceSingleMainContact(studentId, guardian.getId());
        }

        return toResponse(guardian, request.getRelation());
    }

    private void enforceSingleMainContact(Long studentId, Long mainGuardianId) {
        List<StudentGuardianRelation> relations =
                relationMapper.selectList(
                        Wrappers.<StudentGuardianRelation>lambdaQuery()
                                .eq(
                                        StudentGuardianRelation::getTenantId,
                                        TenantContext.getTenantId())
                                .eq(StudentGuardianRelation::getStudentId, studentId));
        for (StudentGuardianRelation rel : relations) {
            Guardian g = guardianMapper.selectById(rel.getGuardianId());
            if (g == null) {
                continue;
            }
            int flag = g.getId().equals(mainGuardianId) ? 1 : 0;
            if (!Objects.equals(g.getIsMainContact(), flag)) {
                g.setIsMainContact(flag);
                guardianMapper.updateById(g);
            }
        }
    }

    private Student requireStudent(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null || !TenantContext.getTenantId().equals(student.getTenantId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        }
        return student;
    }

    private Guardian requireGuardian(Long guardianId) {
        Guardian guardian = guardianMapper.selectById(guardianId);
        if (guardian == null || !TenantContext.getTenantId().equals(guardian.getTenantId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "家长不存在");
        }
        return guardian;
    }

    private GuardianResponse toResponse(Guardian guardian, String relation) {
        return GuardianResponse.builder()
                .id(guardian.getId())
                .name(guardian.getName())
                .phone(guardian.getPhone())
                .isMainContact(guardian.getIsMainContact())
                .canPickup(guardian.getCanPickup())
                .qrCode(guardian.getQrCode())
                .relation(relation)
                .build();
    }
}
