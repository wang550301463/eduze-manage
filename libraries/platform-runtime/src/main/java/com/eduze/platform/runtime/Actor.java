package com.eduze.platform.runtime;

import java.util.Set;

/** Verified identity; branch selection never grants access. */
public record Actor(
        String userId,
        String tenantId,
        Set<String> branchIds,
        Set<String> roles,
        Set<String> permissions) {
    public Actor {
        if (userId == null || tenantId == null) {
            throw new PlatformException(401, "身份无效");
        }
        branchIds = branchIds == null ? Set.of() : Set.copyOf(branchIds);
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean isSuperAdmin() {
        return roles.contains("SUPER_ADMIN");
    }

    public boolean isStaff() {
        if (roles.stream()
                .anyMatch(
                        role ->
                                "PARENT".equalsIgnoreCase(role)
                                        || "VISITOR".equalsIgnoreCase(role))) {
            return false;
        }
        boolean builtInStaff =
                roles.stream()
                        .anyMatch(
                                Set.of(
                                                "SUPER_ADMIN",
                                                "ADMIN",
                                                "PRINCIPAL",
                                                "TEACHER",
                                                "ADVISOR",
                                                "FRONT_DESK",
                                                "RECEPTIONIST",
                                                "STAFF")
                                        ::contains);
        // Custom roles are assigned by identity administrators; only verified permissions count.
        return builtInStaff
                || (roles.stream().anyMatch(role -> !role.isBlank())
                        && permissions.stream().anyMatch(permission -> !permission.isBlank()));
    }

    public void requireBranch(String branchId) {
        if (branchId == null || (!isSuperAdmin() && !branchIds.contains(branchId))) {
            throw new PlatformException(403, "无权访问该校区");
        }
    }

    public void requirePermission(String permission) {
        if (!isSuperAdmin() && !permissions.contains(permission)) {
            throw new PlatformException(403, "无权执行此操作");
        }
    }
}
