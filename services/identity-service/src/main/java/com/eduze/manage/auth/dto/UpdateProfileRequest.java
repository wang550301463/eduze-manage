package com.eduze.manage.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    private String name;
    private String phone;
    private String email;
}
