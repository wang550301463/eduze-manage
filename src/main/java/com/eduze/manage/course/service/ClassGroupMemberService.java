package com.eduze.manage.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.domain.StudentClassGroup;
import com.eduze.manage.course.dto.AddMembersRequest;
import com.eduze.manage.course.dto.ClassMemberResponse;
import com.eduze.manage.course.dto.TransferClassRequest;
import com.eduze.manage.course.mapper.ClassGroupMapper;
import com.eduze.manage.course.mapper.StudentClassGroupMapper;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassGroupMemberService {

    private final ClassGroupMapper classGroupMapper;
    private final StudentClassGroupMapper studentClassGroupMapper;
    private final StudentMapper studentMapper;
    private final ClassGroupService classGroupService;

    public List<ClassMemberResponse> listMembers(Long classGroupId, boolean activeOnly) {
        classGroupService.requireGroup(classGroupId);
        var query = Wrappers.<StudentClassGroup>lambdaQuery()
                .eq(StudentClassGroup::getTenantId, TenantContext.getTenantId())
                .eq(StudentClassGroup::getClassGroupId, classGroupId)
                .orderByDesc(StudentClassGroup::getJoinedAt);
        if (activeOnly) {
            query.isNull(StudentClassGroup::getLeftAt);
        }
        return studentClassGroupMapper.selectList(query).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public void addMembers(Long classGroupId, AddMembersRequest request) {
        ClassGroup group = classGroupService.requireGroup(classGroupId);
        int current = classGroupService.countActiveMembers(classGroupId);
        int incoming = request.getStudentIds().size();
        if (current + incoming > group.getCapacity()) {
            throw new BizException(ErrorCode.CONFLICT, "超出班级容量");
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long studentId : request.getStudentIds()) {
            requireStudent(studentId);
            Long existing = studentClassGroupMapper.selectCount(Wrappers.<StudentClassGroup>lambdaQuery()
                    .eq(StudentClassGroup::getTenantId, TenantContext.getTenantId())
                    .eq(StudentClassGroup::getClassGroupId, classGroupId)
                    .eq(StudentClassGroup::getStudentId, studentId)
                    .isNull(StudentClassGroup::getLeftAt));
            if (existing != null && existing > 0) {
                continue;
            }
            StudentClassGroup membership = new StudentClassGroup();
            membership.setTenantId(TenantContext.getTenantId());
            membership.setStudentId(studentId);
            membership.setClassGroupId(classGroupId);
            membership.setJoinedAt(now);
            studentClassGroupMapper.insert(membership);
        }
    }

    @Transactional
    public void removeMember(Long classGroupId, Long studentId) {
        classGroupService.requireGroup(classGroupId);
        StudentClassGroup membership = studentClassGroupMapper.selectOne(Wrappers.<StudentClassGroup>lambdaQuery()
                .eq(StudentClassGroup::getTenantId, TenantContext.getTenantId())
                .eq(StudentClassGroup::getClassGroupId, classGroupId)
                .eq(StudentClassGroup::getStudentId, studentId)
                .isNull(StudentClassGroup::getLeftAt)
                .last("LIMIT 1"));
        if (membership == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "成员不在该班级");
        }
        membership.setLeftAt(LocalDateTime.now());
        studentClassGroupMapper.updateById(membership);
    }

    @Transactional
    public void transfer(TransferClassRequest request) {
        ClassGroup from = classGroupService.requireGroup(request.getFromClassGroupId());
        ClassGroup to = classGroupService.requireGroup(request.getToClassGroupId());
        int toCount = classGroupService.countActiveMembers(to.getId());
        if (toCount + request.getStudentIds().size() > to.getCapacity()) {
            throw new BizException(ErrorCode.CONFLICT, "目标班级容量不足");
        }
        if (!from.getBranchId().equals(to.getBranchId())) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "转班需在同一校区");
        }
        for (Long studentId : request.getStudentIds()) {
            removeMember(request.getFromClassGroupId(), studentId);
            AddMembersRequest add = new AddMembersRequest();
            add.setStudentIds(List.of(studentId));
            addMembers(request.getToClassGroupId(), add);
        }
    }

    public List<ClassMemberResponse> rosterForLesson(Long classGroupId) {
        return listMembers(classGroupId, true);
    }

    private Student requireStudent(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        }
        return student;
    }

    private ClassMemberResponse toMemberResponse(StudentClassGroup membership) {
        Student student = studentMapper.selectById(membership.getStudentId());
        return ClassMemberResponse.builder()
                .studentId(membership.getStudentId())
                .studentName(student != null ? student.getName() : null)
                .enrollNo(student != null ? student.getEnrollNo() : null)
                .joinedAt(membership.getJoinedAt())
                .leftAt(membership.getLeftAt())
                .build();
    }
}
