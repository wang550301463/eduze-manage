package com.eduze.manage.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInRequest {

    /** manual 必填；qr 可与 qrCode 单独使用由后端解析 */
    private Long lessonId;

    private Long studentId;

    @NotNull private String method;

    private Long guardianId;

    /** QR 扫码时传家长二维码内容 */
    private String qrCode;
}
