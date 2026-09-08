package com.eduze.manage.curriculum.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DimensionResponse {
    private final Long id;
    private final String kind;
    private final String code;
    private final String name;
    private final String description;
    private final Integer orderNo;
}
