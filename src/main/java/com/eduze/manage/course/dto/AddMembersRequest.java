package com.eduze.manage.course.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddMembersRequest {

    @NotEmpty
    private List<Long> studentIds;
}
