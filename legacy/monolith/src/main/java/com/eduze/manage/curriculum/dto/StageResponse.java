package com.eduze.manage.curriculum.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StageResponse {
    private final Long id;
    private final String code;
    private final String name;
    private final Integer ageMin;
    private final Integer ageMax;
    private final Integer orderNo;
    private final String lorenfieldPhase;
    private final String description;
    private final List<DimensionRefResponse> dimensions;

    @Getter
    @Builder
    public static class DimensionRefResponse {
        private final Long id;
        private final String code;
        private final String name;
        private final String kind;
        private final Integer weight;
    }
}
