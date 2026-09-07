package com.eduze.manage.auth.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignBranchesRequest {

    @NotNull
    private List<Long> branchIds;
}
