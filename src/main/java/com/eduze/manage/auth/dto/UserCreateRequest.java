package com.eduze.manage.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    private String phone;
    private String email;
}
