package com.eduze.manage.lesson.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.domain.LessonStudent;
import com.eduze.manage.lesson.dto.LessonStudentRequest;
import com.eduze.manage.lesson.dto.LessonStudentResponse;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.lesson.mapper.LessonStudentMapper;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDateTime;
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
public class LessonStudentService {

    private final LessonStudentMapper lessonStudentMapper;
    private final LessonMapper lessonMapper;
    private final StudentMapper studentMapper;

    public List<LessonStudentResponse> list(Long lessonId) {
        List<LessonStudent> rows =
                lessonStudentMapper.selectList(
                        new LambdaQueryWrapper<LessonStudent>()
                                .eq(LessonStudent::getLessonId, lessonId));
        if (rows.isEmpty()) return List.of();
        Set<Long> studentIds =
                rows.stream().map(LessonStudent::getStudentId).collect(Collectors.toSet());
        Map<Long, String> studentNames =
                studentMapper.selectBatchIds(studentIds).stream()
                        .collect(Collectors.toMap(Student::getId, Student::getName, (a, b) -> a));
        return rows.stream()
                .map(
                        r ->
                                LessonStudentResponse.builder()
                                        .id(r.getId())
                                        .lessonId(r.getLessonId())
                                        .studentId(r.getStudentId())
                                        .studentName(studentNames.get(r.getStudentId()))
                                        .subscriptionId(r.getSubscriptionId())
                                        .source(r.getSource())
                                        .status(r.getStatus())
                                        .note(r.getNote())
                                        .build())
                .toList();
    }

    @Transactional
    public LessonStudentResponse addStudent(Long lessonId, LessonStudentRequest req) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课次不存在");
        }
        Student student = studentMapper.selectById(req.getStudentId());
        if (student == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "学员不存在");
        }
        LessonStudent ls = new LessonStudent();
        ls.setTenantId(TenantContext.getTenantId());
        ls.setBranchId(lesson.getBranchId());
        ls.setLessonId(lessonId);
        ls.setStudentId(req.getStudentId());
        ls.setSource(req.getSource() != null ? req.getSource() : "MANUAL");
        ls.setStatus("BOOKED");
        ls.setNote(req.getNote());
        try {
            lessonStudentMapper.insert(ls);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "该学员已在名单中");
        }
        return LessonStudentResponse.builder()
                .id(ls.getId())
                .lessonId(ls.getLessonId())
                .studentId(ls.getStudentId())
                .studentName(student.getName())
                .subscriptionId(ls.getSubscriptionId())
                .source(ls.getSource())
                .status(ls.getStatus())
                .note(ls.getNote())
                .build();
    }

    @Transactional
    public void removeStudent(Long lessonId, Long studentId) {
        LessonStudent existing =
                lessonStudentMapper.selectOne(
                        new LambdaQueryWrapper<LessonStudent>()
                                .eq(LessonStudent::getLessonId, lessonId)
                                .eq(LessonStudent::getStudentId, studentId));
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员未在名单中");
        }
        existing.setStatus("CANCELLED");
        existing.setRemovedAt(LocalDateTime.now());
        lessonStudentMapper.updateById(existing);
    }
}
