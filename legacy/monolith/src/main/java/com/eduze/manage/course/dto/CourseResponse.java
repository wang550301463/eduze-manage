package com.eduze.manage.course.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CourseResponse {

    private final Long id;
    private final Long tenantId;
    private final String name;
    private final Integer ageMin;
    private final Integer ageMax;
    private final Integer lessonMinutes;
    private final String coverUrl;
    private final String description;
}
