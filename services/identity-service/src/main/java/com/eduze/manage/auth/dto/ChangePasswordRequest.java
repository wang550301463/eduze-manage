package com.eduze.manage.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank private String oldPassword;

    @NotBlank private String newPassword;
}
