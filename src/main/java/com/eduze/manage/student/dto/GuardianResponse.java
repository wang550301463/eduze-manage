package com.eduze.manage.student.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuardianResponse {

    private final Long id;
    private final String name;
    private final String phone;
    private final Integer isMainContact;
    private final Integer canPickup;
    private final String qrCode;
    private final String relation;
}
