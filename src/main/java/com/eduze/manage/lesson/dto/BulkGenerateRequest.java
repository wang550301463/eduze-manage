package com.eduze.manage.lesson.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 老师为核心的批量生成请求：基于 TeacherAvailability 模板生成未来 N 周课次。
 * 兼容旧字段（保留 classGroupId/weekdays 等可选）。
 */
@Getter
@Setter
public class BulkGenerateRequest {

    /** 起始日期（含），默认下周一。 */
    @NotNull
    private LocalDate fromDate;

    /** 生成周数，1..8 */
    @NotNull
    @Min(1)
    @Max(8)
    private Integer weeks;

    /** 指定老师范围；null/空 = 全部启用老师 */
    private List<Long> teacherIds;

    /** 指定校区；null = 不限 */
    private Long branchId;

    /** 跳过的节假日（YYYY-MM-DD） */
    private List<LocalDate> holidays;
}
