package com.eduze.manage.student.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuardianSummaryResponse {

    private Long id;
    private String name;
    private String phone;
    private String relation;
    private Integer canPickup;
    private Integer isMainContact;
}
