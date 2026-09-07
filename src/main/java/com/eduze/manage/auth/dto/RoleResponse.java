package com.eduze.manage.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleResponse {

    private final Long id;
    private final String code;
    private final String name;
    private final Integer isBuiltin;
}
