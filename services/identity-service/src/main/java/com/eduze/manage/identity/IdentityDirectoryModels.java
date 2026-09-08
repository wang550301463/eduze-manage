package com.eduze.manage.identity;

import java.util.List;
import java.util.Set;

public final class IdentityDirectoryModels {
    private IdentityDirectoryModels() {}

    public record UserView(
            Long id,
            Long tenantId,
            Long branchId,
            String name,
            String username,
            Integer status,
            List<Long> branchIds,
            Set<String> roles) {}

    public record BranchView(Long id, Long tenantId, String name, String code) {}
}
