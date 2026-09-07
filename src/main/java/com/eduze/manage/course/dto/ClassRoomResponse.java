package com.eduze.manage.course.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassRoomResponse {

    private final Long id;
    private final Long tenantId;
    private final Long branchId;
    private final String name;
    private final Integer capacity;
    private final String note;
}
