package com.eduze.manage.student.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuardianUpsertRequest {

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 20, message = "手机号过长")
    private String phone;

    @Size(max = 32)
    private String relation;

    @Min(0)
    @Max(1)
    private Integer isMainContact;

    @Min(0)
    @Max(1)
    private Integer canPickup;
}
