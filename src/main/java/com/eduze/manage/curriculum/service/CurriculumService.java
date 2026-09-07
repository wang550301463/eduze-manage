package com.eduze.manage.curriculum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduze.manage.curriculum.domain.CurriculumDimension;
import com.eduze.manage.curriculum.domain.CurriculumStage;
import com.eduze.manage.curriculum.domain.StageDimension;
import com.eduze.manage.curriculum.dto.DimensionResponse;
import com.eduze.manage.curriculum.dto.StageResponse;
import com.eduze.manage.curriculum.mapper.CurriculumDimensionMapper;
import com.eduze.manage.curriculum.mapper.CurriculumStageMapper;
import com.eduze.manage.curriculum.mapper.StageDimensionMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurriculumService {

    private final CurriculumStageMapper stageMapper;
    private final CurriculumDimensionMapper dimensionMapper;
    private final StageDimensionMapper stageDimensionMapper;

    public List<StageResponse> listStages() {
        List<CurriculumStage> stages = stageMapper.selectList(
                new LambdaQueryWrapper<CurriculumStage>().orderByAsc(CurriculumStage::getOrderNo));
        if (stages.isEmpty()) {
            return List.of();
        }
        List<StageDimension> mappings = stageDimensionMapper.selectList(null);
        Map<Long, CurriculumDimension> dimById = dimensionMapper.selectList(null).stream()
                .collect(Collectors.toMap(CurriculumDimension::getId, d -> d));
        Map<Long, List<StageDimension>> byStage = mappings.stream()
                .collect(Collectors.groupingBy(StageDimension::getStageId));

        return stages.stream().map(s -> {
            List<StageResponse.DimensionRefResponse> refs = byStage.getOrDefault(s.getId(), List.of()).stream()
                    .map(sd -> {
                        CurriculumDimension d = dimById.get(sd.getDimensionId());
                        if (d == null) return null;
                        return StageResponse.DimensionRefResponse.builder()
                                .id(d.getId())
                                .code(d.getCode())
                                .name(d.getName())
                                .kind(d.getKind())
                                .weight(sd.getWeight())
                                .build();
                    })
                    .filter(r -> r != null)
                    .toList();
            return StageResponse.builder()
                    .id(s.getId())
                    .code(s.getCode())
                    .name(s.getName())
                    .ageMin(s.getAgeMin())
                    .ageMax(s.getAgeMax())
                    .orderNo(s.getOrderNo())
                    .lorenfieldPhase(s.getLorenfieldPhase())
                    .description(s.getDescription())
                    .dimensions(refs)
                    .build();
        }).toList();
    }

    public List<DimensionResponse> listDimensions(String kind) {
        LambdaQueryWrapper<CurriculumDimension> wrapper = new LambdaQueryWrapper<CurriculumDimension>()
                .orderByAsc(CurriculumDimension::getKind)
                .orderByAsc(CurriculumDimension::getOrderNo);
        if (kind != null && !kind.isBlank()) {
            wrapper.eq(CurriculumDimension::getKind, kind);
        }
        return dimensionMapper.selectList(wrapper).stream()
                .map(d -> DimensionResponse.builder()
                        .id(d.getId())
                        .kind(d.getKind())
                        .code(d.getCode())
                        .name(d.getName())
                        .description(d.getDescription())
                        .orderNo(d.getOrderNo())
                        .build())
                .toList();
    }
}
