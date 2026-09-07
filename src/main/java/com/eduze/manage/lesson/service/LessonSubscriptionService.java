package com.eduze.manage.lesson.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.lesson.domain.LessonStudent;
import com.eduze.manage.lesson.domain.LessonSubscription;
import com.eduze.manage.lesson.dto.SubscriptionRequest;
import com.eduze.manage.lesson.dto.SubscriptionResponse;
import com.eduze.manage.lesson.mapper.LessonStudentMapper;
import com.eduze.manage.lesson.mapper.LessonSubscriptionMapper;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import com.eduze.manage.teacher.mapper.TeacherAvailabilityMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LessonSubscriptionService {

    private final LessonSubscriptionMapper subscriptionMapper;
    private final TeacherAvailabilityMapper availabilityMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final LessonStudentMapper lessonStudentMapper;

    public List<SubscriptionResponse> list(Long studentId, Long teacherId) {
        LambdaQueryWrapper<LessonSubscription> wrapper = new LambdaQueryWrapper<>();
        if (studentId != null) wrapper.eq(LessonSubscription::getStudentId, studentId);
        if (teacherId != null) wrapper.eq(LessonSubscription::getTeacherId, teacherId);
        wrapper.orderByDesc(LessonSubscription::getId);
        return toResponses(subscriptionMapper.selectList(wrapper));
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest req) {
        TeacherAvailability avail = availabilityMapper.selectById(req.getTeacherAvailabilityId());
        if (avail == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "可用时段不存在");
        }
        Student student = studentMapper.selectById(req.getStudentId());
        if (student == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "学员不存在");
        }
        if (!student.getBranchId().equals(avail.getBranchId())) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "学员与可用时段不在同一校区");
        }
        LessonSubscription entity = new LessonSubscription();
        entity.setTenantId(TenantContext.getTenantId());
        entity.setBranchId(student.getBranchId());
        entity.setStudentId(req.getStudentId());
        entity.setTeacherId(avail.getTeacherId());
        entity.setTeacherAvailabilityId(req.getTeacherAvailabilityId());
        entity.setValidFrom(req.getValidFrom());
        entity.setValidTo(req.getValidTo());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        entity.setSource(req.getSource() != null ? req.getSource() : "NORMAL");
        entity.setNote(req.getNote());
        try {
            subscriptionMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "已存在相同订阅");
        }
        return toResponse(entity, avail, student);
    }

    @Transactional
    public SubscriptionResponse update(Long id, SubscriptionRequest req) {
        LessonSubscription entity = subscriptionMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "订阅不存在");
        }
        entity.setValidFrom(req.getValidFrom());
        entity.setValidTo(req.getValidTo());
        if (req.getStatus() != null) entity.setStatus(req.getStatus());
        if (req.getSource() != null) entity.setSource(req.getSource());
        if (req.getNote() != null) entity.setNote(req.getNote());
        subscriptionMapper.updateById(entity);
        return toResponse(entity, null, null);
    }

    /**
     * 退订：将订阅 status 置 0、validTo=今天；并将所有未来日期且 BOOKED 的名单软删除。
     */
    @Transactional
    public void delete(Long id) {
        LessonSubscription entity = subscriptionMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "订阅不存在");
        }
        entity.setStatus(0);
        entity.setValidTo(LocalDate.now());
        subscriptionMapper.updateById(entity);

        // 取消未来名单（subscription_id = id）— 使用 update 模式标记 status=CANCELLED
        LambdaUpdateWrapper<LessonStudent> u = new LambdaUpdateWrapper<LessonStudent>()
                .eq(LessonStudent::getSubscriptionId, id)
                .eq(LessonStudent::getStatus, "BOOKED")
                .set(LessonStudent::getStatus, "CANCELLED");
        lessonStudentMapper.update(null, u);
    }

    private List<SubscriptionResponse> toResponses(List<LessonSubscription> subs) {
        if (subs.isEmpty()) return List.of();
        Set<Long> availIds = subs.stream().map(LessonSubscription::getTeacherAvailabilityId).collect(Collectors.toSet());
        Set<Long> studentIds = subs.stream().map(LessonSubscription::getStudentId).collect(Collectors.toSet());
        Set<Long> teacherIds = new HashSet<>();
        subs.forEach(s -> teacherIds.add(s.getTeacherId()));
        Map<Long, TeacherAvailability> availMap = availabilityMapper.selectBatchIds(availIds).stream()
                .collect(Collectors.toMap(TeacherAvailability::getId, t -> t));
        Map<Long, String> studentNames = studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Student::getName, (a, b) -> a));
        Map<Long, String> teacherNames = userMapper.selectBatchIds(teacherIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        return subs.stream().map(s -> {
            TeacherAvailability avail = availMap.get(s.getTeacherAvailabilityId());
            return SubscriptionResponse.builder()
                    .id(s.getId())
                    .studentId(s.getStudentId())
                    .studentName(studentNames.get(s.getStudentId()))
                    .teacherId(s.getTeacherId())
                    .teacherName(teacherNames.get(s.getTeacherId()))
                    .teacherAvailabilityId(s.getTeacherAvailabilityId())
                    .branchId(s.getBranchId())
                    .dayOfWeek(avail != null ? avail.getDayOfWeek() : null)
                    .startMinute(avail != null ? avail.getStartMinute() : null)
                    .endMinute(avail != null ? avail.getEndMinute() : null)
                    .validFrom(s.getValidFrom())
                    .validTo(s.getValidTo())
                    .status(s.getStatus())
                    .source(s.getSource())
                    .note(s.getNote())
                    .build();
        }).toList();
    }

    private SubscriptionResponse toResponse(LessonSubscription entity, TeacherAvailability avail, Student student) {
        if (avail == null) avail = availabilityMapper.selectById(entity.getTeacherAvailabilityId());
        if (student == null) student = studentMapper.selectById(entity.getStudentId());
        User teacher = userMapper.selectById(entity.getTeacherId());
        return SubscriptionResponse.builder()
                .id(entity.getId())
                .studentId(entity.getStudentId())
                .studentName(student != null ? student.getName() : null)
                .teacherId(entity.getTeacherId())
                .teacherName(teacher != null ? teacher.getName() : null)
                .teacherAvailabilityId(entity.getTeacherAvailabilityId())
                .branchId(entity.getBranchId())
                .dayOfWeek(avail != null ? avail.getDayOfWeek() : null)
                .startMinute(avail != null ? avail.getStartMinute() : null)
                .endMinute(avail != null ? avail.getEndMinute() : null)
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .source(entity.getSource())
                .note(entity.getNote())
                .build();
    }
}
