package com.eduze.manage.course.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferClassRequest {

    @NotEmpty
    private List<Long> studentIds;

    @NotNull
    private Long fromClassGroupId;

    @NotNull
    private Long toClassGroupId;
}
