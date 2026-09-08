package com.eduze.platform.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ActorStaffTest {
    private Actor actor(Set<String> roles, Set<String> permissions) {
        return new Actor("1", "1", Set.of("2"), roles, permissions);
    }

    @Test
    void customRoleWithVerifiedPermissionsIsStaffWithoutGrantingOtherPermissions() {
        Actor custom = actor(Set.of("STUDIO_ASSISTANT"), Set.of("student:read"));
        assertTrue(custom.isStaff());
        assertFalse(custom.isSuperAdmin());
        assertDoesNotThrow(() -> custom.requirePermission("student:read"));
        assertThrows(PlatformException.class, () -> custom.requirePermission("student:write"));
        assertThrows(PlatformException.class, () -> custom.requireBranch("3"));
    }

    @Test
    void parentVisitorAndUnassignedIdentitiesNeverBecomeStaff() {
        assertFalse(actor(Set.of("PARENT"), Set.of()).isStaff());
        assertFalse(actor(Set.of("PARENT", "TEACHER"), Set.of("student:read")).isStaff());
        assertFalse(actor(Set.of("VISITOR", "CUSTOM"), Set.of("student:read")).isStaff());
        assertFalse(actor(Set.of(), Set.of("student:read")).isStaff());
        assertFalse(actor(Set.of("CUSTOM"), Set.of()).isStaff());
        assertFalse(actor(Set.of(""), Set.of("student:read")).isStaff());
        assertTrue(actor(Set.of("TEACHER"), Set.of()).isStaff());
    }
}
