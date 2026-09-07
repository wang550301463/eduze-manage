package com.eduze.manage.course.dto;

import jakarta.validation.Valid;
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

    /** 容量由绑定时段镜像；请求中若传入会被覆盖。 */
    private Integer capacity;

    private Integer status;

    /** 标签颜色：#RRGGBB */
    @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "颜色须为 #RRGGBB 格式")
    private String tagColor;

    /** 绑定已有时段；与 nestedAvailability 二选一（新建必填其一）。 */
    private Long teacherAvailabilityId;

    /** 现场新建时段载荷；非空时服务端先建 availability 再绑定。 */
    @Valid
    private NestedTeacherAvailabilityRequest nestedAvailability;
}
