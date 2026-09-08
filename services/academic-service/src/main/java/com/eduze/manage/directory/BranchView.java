package com.eduze.manage.directory;

import lombok.Data;

@Data
public class BranchView {
    private Long id;
    private Long tenantId;
    private String name;
    private String code;
}
