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
    private final Long teacherAvailabilityId;
    private final Integer dayOfWeek;
    private final Integer startMinute;
    private final Integer endMinute;

    /** 绑定时段老师姓名（与班主任一致）。 */
    private final String teacherName;

    /** 换绑后提示重新 bulk-generate（可选）。 */
    private final String message;
}
