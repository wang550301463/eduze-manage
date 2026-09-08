package com.eduze.manage.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    private String name;
    private String phone;
    private String email;
    private Integer status;
}
