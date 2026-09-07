package com.eduze.manage.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassGroupRequest {

    @NotNull
    private Long branchId;

    @NotBlank
    @Size(max = 128)
    private String name;

    /** 班级降级为"分组标签"后，课程产品可选。 */
    private Long courseId;

    private Long headTeacherId;

    @NotNull
    private Integer capacity;

    private Integer status;

    /** 标签颜色：#RRGGBB */
    @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "颜色须为 #RRGGBB 格式")
    private String tagColor;
}
