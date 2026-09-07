package com.eduze.manage.auth.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private final Long id;
    private final String username;
    private final String name;
    private final String phone;
    private final String email;
    private final Integer status;
    private final List<String> roles;
    private final List<Long> branchIds;
}
