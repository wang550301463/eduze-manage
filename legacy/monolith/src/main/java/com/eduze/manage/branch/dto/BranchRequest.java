package com.eduze.manage.branch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchRequest {

    @NotBlank private String name;

    @NotBlank private String code;

    private String address;
    private String phone;
    private Integer status;
}
