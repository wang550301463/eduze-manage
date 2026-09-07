package com.eduze.manage.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherSummaryResponse {
    private final Long id;
    private final String username;
    private final String name;
    private final Long branchId;
}
