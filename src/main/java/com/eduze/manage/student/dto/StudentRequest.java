package com.eduze.manage.student.dto;

import com.eduze.manage.common.validation.TenantUnique;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentRequest {

    @NotNull
    private Long branchId;

    @NotBlank
    @Size(min = 1, max = 64, message = "入园编号长度须在 1–64 字以内")
    @TenantUnique(table = "t_student", column = "enroll_no")
    private String enrollNo;

    @NotBlank
    @Size(min = 1, max = 64)
    private String name;

    @Min(0)
    @Max(2)
    private Integer gender;

    private LocalDate birthday;
    private LocalDate enrollDate;

    @Min(1)
    @Max(3)
    private Integer status;

    @Size(max = 512)
    private String allergy;

    @Size(max = 1024)
    private String healthNote;

    @Size(max = 128)
    private String emergencyContact;

    /** 第一期允许留空；填写时建议为 11 位手机号 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "紧急联系电话须为 11 位手机号或留空")
    private String emergencyPhone;

    @Size(max = 512)
    private String avatarUrl;

    @NotNull(message = "主带老师必填")
    private Long mentorTeacherId;

    private Long currentStageId;

    @Valid
    private List<InitialSubscription> initialSubscriptions;

    @Getter
    @Setter
    public static class InitialSubscription {
        @NotNull
        private Long teacherAvailabilityId;

        @NotNull
        private LocalDate validFrom;

        private LocalDate validTo;
    }
}
