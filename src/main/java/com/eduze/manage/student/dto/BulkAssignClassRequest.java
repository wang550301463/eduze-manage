package com.eduze.manage.student.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkAssignClassRequest {

    @NotEmpty
    private List<Long> studentIds;

    @NotNull
    private Long classGroupId;
}
