package com.eduze.manage.directory;

import java.util.List;
import java.util.Set;
import lombok.Data;

/** Immutable wire data owned by identity; no persistence mapping. */
@Data
public class UserView {
    private Long id;
    private Long tenantId;
    private Long branchId;
    private String name;
    private String username;
    private Integer status;
    private List<Long> branchIds = List.of();
    private Set<String> roles = Set.of();
}
