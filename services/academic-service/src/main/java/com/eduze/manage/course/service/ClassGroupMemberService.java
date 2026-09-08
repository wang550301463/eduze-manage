package com.eduze.manage.course.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.course.domain.ClassGroup;
import com.eduze.manage.course.domain.StudentClassGroup;
import com.eduze.manage.course.dto.AddMembersRequest;
import com.eduze.manage.course.dto.ClassMemberResponse;
import com.eduze.manage.course.dto.TransferClassRequest;
import com.eduze.manage.course.mapper.StudentClassGroupMapper;
import com.eduze.manage.lesson.domain.LessonSubscription;
import com.eduze.manage.lesson.mapper.LessonSubscriptionMapper;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.dto.AssignMentorRequest;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.student.service.StudentMentorService;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import com.eduze.manage.teacher.mapper.TeacherAvailabilityMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassGroupMemberService {

    private final StudentClassGroupMapper studentClassGroupMapper;
    private final com.eduze.manage.lesson.service.LessonStudentService roster;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final com.eduze.manage.integration.RosterCapacity rosterCapacity;
    private final StudentMapper studentMapper;
    private final ClassGroupService classGroupService;
    private final TeacherAvailabilityMapper teacherAvailabilityMapper;
    private final LessonSubscriptionMapper lessonSubscriptionMapper;
    private final StudentMentorService studentMentorService;

    public List<ClassMemberResponse> listMembers(Long classGroupId, boolean activeOnly) {
        classGroupService.requireGroup(classGroupId);
        var query =
                Wrappers.<StudentClassGroup>lambdaQuery()
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
        jdbc.queryForList(
                "SELECT id FROM t_class_group WHERE tenant_id=? AND id=? AND deleted_at=0 FOR UPDATE",
                TenantContext.getTenantId(),
                classGroupId);
        ClassGroup group = classGroupService.requireGroup(classGroupId);
        TeacherAvailability avail = requireBoundAvailability(group);
        int current = classGroupService.countActiveMembers(classGroupId);
        int incoming = request.getStudentIds().size();
        if (current + incoming > group.getCapacity()) {
            throw new BizException(ErrorCode.CONFLICT, "超出班级容量");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        for (Long studentId : request.getStudentIds()) {
            Student student = requireStudent(studentId);
            if (!Objects.equals(student.getBranchId(), group.getBranchId()))
                throw new BizException(ErrorCode.FORBIDDEN, "学员校区不属于班级");
            Long existing =
                    studentClassGroupMapper.selectCount(
                            Wrappers.<StudentClassGroup>lambdaQuery()
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

            alignMentor(student, avail.getTeacherId());
            ensureActiveSubscription(studentId, group.getBranchId(), avail, today);
            var lessons =
                    jdbc.queryForList(
                            "SELECT id FROM t_lesson WHERE tenant_id=? AND class_group_id=? AND deleted_at=0 AND status='SCHEDULED' AND start_at>NOW(3) ORDER BY id",
                            String.class,
                            TenantContext.getTenantId(),
                            classGroupId);
            for (String lessonId : lessons) {
                rosterCapacity.lock(TenantContext.getTenantId().toString(), lessonId);
                Integer already =
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_lesson_student WHERE tenant_id=? AND lesson_id=? AND student_id=? AND deleted_at=0 AND status='BOOKED'",
                                Integer.class,
                                TenantContext.getTenantId(),
                                lessonId,
                                studentId);
                if (already > 0) continue;
                var requestRow = new com.eduze.manage.lesson.dto.LessonStudentRequest();
                requestRow.setStudentId(studentId);
                requestRow.setSource("SUBSCRIPTION");
                roster.addStudent(Long.valueOf(lessonId), requestRow);
            }
        }
    }

    @Transactional
    public void removeMember(Long classGroupId, Long studentId) {
        ClassGroup group = classGroupService.requireGroup(classGroupId);
        StudentClassGroup membership =
                studentClassGroupMapper.selectOne(
                        Wrappers.<StudentClassGroup>lambdaQuery()
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
        if (group.getTeacherAvailabilityId() != null) {
            deactivateSubscription(studentId, group.getTeacherAvailabilityId());
            roster.cancelFuture(studentId, group.getTeacherAvailabilityId());
        }
    }

    @Transactional
    public void transfer(TransferClassRequest request) {
        ClassGroup from = classGroupService.requireGroup(request.getFromClassGroupId());
        ClassGroup to = classGroupService.requireGroup(request.getToClassGroupId());
        // 仅目标组必须已绑定；允许从历史未绑定分组迁出
        requireBoundAvailability(to);
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

    private TeacherAvailability requireBoundAvailability(ClassGroup group) {
        if (group.getTeacherAvailabilityId() == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "该分组尚未绑定老师可用时段，无法进组/调班");
        }
        TeacherAvailability avail =
                teacherAvailabilityMapper.selectById(group.getTeacherAvailabilityId());
        if (avail == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分组绑定的可用时段不存在");
        }
        return avail;
    }

    private void alignMentor(Student student, Long teacherId) {
        if (Objects.equals(student.getMentorTeacherId(), teacherId)) {
            return;
        }
        AssignMentorRequest req = new AssignMentorRequest();
        req.setToTeacherId(teacherId);
        req.setReason("进组同步主带");
        req.setKeepSubscriptions(true);
        studentMentorService.changeMentor(student.getId(), req);
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

    private void deactivateSubscription(Long studentId, Long teacherAvailabilityId) {
        List<LessonSubscription> subs =
                lessonSubscriptionMapper.selectList(
                        Wrappers.<LessonSubscription>lambdaQuery()
                                .eq(LessonSubscription::getTenantId, TenantContext.getTenantId())
                                .eq(LessonSubscription::getStudentId, studentId)
                                .eq(
                                        LessonSubscription::getTeacherAvailabilityId,
                                        teacherAvailabilityId)
                                .eq(LessonSubscription::getStatus, 1));
        LocalDate today = LocalDate.now();
        for (LessonSubscription sub : subs) {
            sub.setStatus(0);
            sub.setValidTo(today);
            lessonSubscriptionMapper.updateById(sub);
        }
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
