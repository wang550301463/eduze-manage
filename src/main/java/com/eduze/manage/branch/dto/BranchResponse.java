package com.eduze.manage.branch.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BranchResponse {

    private final Long id;
    private final String name;
    private final String code;
    private final String address;
    private final String phone;
    private final Integer status;
}
