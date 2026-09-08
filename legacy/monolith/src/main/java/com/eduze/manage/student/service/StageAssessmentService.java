package com.eduze.manage.student.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.curriculum.domain.CurriculumStage;
import com.eduze.manage.curriculum.mapper.CurriculumStageMapper;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.domain.StudentStageAssessment;
import com.eduze.manage.student.dto.StageAssessmentRequest;
import com.eduze.manage.student.dto.StageAssessmentResponse;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.student.mapper.StudentStageAssessmentMapper;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StageAssessmentService {

    private final StudentStageAssessmentMapper mapper;
    private final StudentMapper studentMapper;
    private final CurriculumStageMapper stageMapper;

    @Transactional
    public StageAssessmentResponse create(Long studentId, StageAssessmentRequest req) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        }
        CurriculumStage stage = stageMapper.selectById(req.getStageId());
        if (stage == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "阶段不存在");
        }
        StudentStageAssessment a = new StudentStageAssessment();
        a.setTenantId(TenantContext.getTenantId());
        a.setBranchId(student.getBranchId());
        a.setStudentId(studentId);
        a.setStageId(req.getStageId());
        a.setAssessedAt(req.getAssessedAt());
        if (req.getScores() != null && !req.getScores().isEmpty()) {
            a.setScoresJson(JSON.toJSONString(req.getScores()));
        }
        a.setComment(req.getComment());
        mapper.insert(a);
        return toResponse(a, stage);
    }

    public List<StageAssessmentResponse> list(Long studentId) {
        List<StudentStageAssessment> rows =
                mapper.selectList(
                        new LambdaQueryWrapper<StudentStageAssessment>()
                                .eq(StudentStageAssessment::getStudentId, studentId)
                                .orderByDesc(StudentStageAssessment::getAssessedAt));
        return rows.stream().map(this::toResponseWithStage).toList();
    }

    private StageAssessmentResponse toResponseWithStage(StudentStageAssessment a) {
        CurriculumStage stage =
                a.getStageId() == null ? null : stageMapper.selectById(a.getStageId());
        return toResponse(a, stage);
    }

    private StageAssessmentResponse toResponse(StudentStageAssessment a, CurriculumStage stage) {
        Map<String, Integer> scores = null;
        if (a.getScoresJson() != null && !a.getScoresJson().isBlank()) {
            scores =
                    JSON.parseObject(
                            a.getScoresJson(), new TypeReference<Map<String, Integer>>() {});
        }
        return StageAssessmentResponse.builder()
                .id(a.getId())
                .studentId(a.getStudentId())
                .stageId(a.getStageId())
                .stageCode(stage != null ? stage.getCode() : null)
                .stageName(stage != null ? stage.getName() : null)
                .assessedAt(a.getAssessedAt())
                .assessedBy(a.getAssessedBy())
                .scores(scores)
                .comment(a.getComment())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
