package com.eduze.manage.course.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassGroupResponse {

    private final Long id;
    private final Long tenantId;
    private final Long branchId;
    private final String name;
    private final Long courseId;
    private final String courseName;
    private final Long headTeacherId;
    private final String headTeacherName;
    private final Integer capacity;
    private final Integer currentCount;
    private final Integer status;
    private final String tagColor;
}
