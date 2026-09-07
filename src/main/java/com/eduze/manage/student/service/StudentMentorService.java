package com.eduze.manage.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.lesson.domain.LessonSubscription;
import com.eduze.manage.lesson.mapper.LessonSubscriptionMapper;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.domain.StudentMentorHistory;
import com.eduze.manage.student.dto.AssignMentorRequest;
import com.eduze.manage.student.dto.MentorHistoryResponse;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.student.mapper.StudentMentorHistoryMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentMentorService {

    private final StudentMapper studentMapper;
    private final StudentMentorHistoryMapper historyMapper;
    private final UserMapper userMapper;
    private final LessonSubscriptionMapper subscriptionMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 校验 mentor 是否合法（同 tenant + branch + TEACHER 角色 + 启用）。返回 mentor User 实体。
     */
    public User requireMentor(Long teacherId, Long branchId) {
        if (teacherId == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "主带老师必填");
        }
        Long tenantId = TenantContext.getTenantId();
        Integer roleCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM t_user u
                JOIN t_user_role ur ON ur.user_id = u.id
                JOIN t_role r ON r.id = ur.role_id AND r.code = 'TEACHER'
                WHERE u.tenant_id = ? AND u.id = ? AND u.deleted_at = 0 AND u.status = 1
                """,
                Integer.class, tenantId, teacherId);
        if (roleCount == null || roleCount == 0) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "主带老师不存在或不是 TEACHER 角色");
        }
        User user = userMapper.selectById(teacherId);
        if (user == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "主带老师不存在");
        }
        if (branchId != null) {
            // 通过 user_branch 多对多关系校验，user.branch_id 仅作为兜底
            Integer branchCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM t_user_branch
                    WHERE tenant_id = ? AND user_id = ? AND branch_id = ?
                    """,
                    Integer.class, tenantId, teacherId, branchId);
            boolean inBranchScope = (branchCount != null && branchCount > 0)
                    || (user.getBranchId() != null && user.getBranchId().equals(branchId));
            if (!inBranchScope) {
                throw new BizException(ErrorCode.VALIDATION_FAILED, "主带老师不属于该校区");
            }
        }
        return user;
    }

    /**
     * 记录主带变更历史。
     */
    @Transactional
    public void recordHistory(Long studentId, Long branchId, Long fromTeacherId, Long toTeacherId, String reason) {
        StudentMentorHistory hist = new StudentMentorHistory();
        hist.setTenantId(TenantContext.getTenantId());
        hist.setBranchId(branchId);
        hist.setStudentId(studentId);
        hist.setFromTeacherId(fromTeacherId);
        hist.setToTeacherId(toTeacherId);
        hist.setReason(reason);
        hist.setChangedAt(LocalDateTime.now());
        historyMapper.insert(hist);
    }

    /**
     * 换主带老师 + 写历史 + 可选取消旧订阅。
     */
    @Transactional
    public void changeMentor(Long studentId, AssignMentorRequest req) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        }
        requireMentor(req.getToTeacherId(), student.getBranchId());
        Long oldTeacherId = student.getMentorTeacherId();
        if (req.getToTeacherId().equals(oldTeacherId)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "新主带老师与原主带相同");
        }
        student.setMentorTeacherId(req.getToTeacherId());
        studentMapper.updateById(student);
        recordHistory(studentId, student.getBranchId(), oldTeacherId, req.getToTeacherId(), req.getReason());

        boolean keep = Boolean.TRUE.equals(req.getKeepSubscriptions());
        if (!keep && oldTeacherId != null) {
            LambdaUpdateWrapper<LessonSubscription> u = new LambdaUpdateWrapper<LessonSubscription>()
                    .eq(LessonSubscription::getStudentId, studentId)
                    .eq(LessonSubscription::getTeacherId, oldTeacherId)
                    .eq(LessonSubscription::getStatus, 1)
                    .set(LessonSubscription::getStatus, 0)
                    .set(LessonSubscription::getValidTo, LocalDate.now());
            subscriptionMapper.update(null, u);
        }
    }

    public List<MentorHistoryResponse> history(Long studentId) {
        List<StudentMentorHistory> histories = historyMapper.selectList(
                new LambdaQueryWrapper<StudentMentorHistory>()
                        .eq(StudentMentorHistory::getStudentId, studentId)
                        .orderByDesc(StudentMentorHistory::getChangedAt));
        Set<Long> teacherIds = new HashSet<>();
        histories.forEach(h -> {
            if (h.getFromTeacherId() != null) teacherIds.add(h.getFromTeacherId());
            if (h.getToTeacherId() != null) teacherIds.add(h.getToTeacherId());
        });
        Map<Long, String> nameById = teacherIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(teacherIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        return histories.stream()
                .map(h -> MentorHistoryResponse.builder()
                        .id(h.getId())
                        .fromTeacherId(h.getFromTeacherId())
                        .fromTeacherName(h.getFromTeacherId() == null ? null : nameById.get(h.getFromTeacherId()))
                        .toTeacherId(h.getToTeacherId())
                        .toTeacherName(nameById.get(h.getToTeacherId()))
                        .reason(h.getReason())
                        .changedAt(h.getChangedAt())
                        .operatorId(h.getOperatorId())
                        .build())
                .toList();
    }
}
