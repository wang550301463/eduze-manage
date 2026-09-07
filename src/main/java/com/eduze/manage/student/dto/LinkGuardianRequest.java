package com.eduze.manage.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkGuardianRequest {

    @NotBlank
    @Size(max = 32)
    private String relation;
}
