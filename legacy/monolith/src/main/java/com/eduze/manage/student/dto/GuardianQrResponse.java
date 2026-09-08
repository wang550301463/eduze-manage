package com.eduze.manage.student.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuardianQrResponse {

    private Long guardianId;
    private String qrCode;
}
