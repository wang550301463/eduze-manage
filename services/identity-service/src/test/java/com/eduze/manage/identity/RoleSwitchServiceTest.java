package com.eduze.manage.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.manage.auth.dto.LoginResponse;
import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.auth.security.CustomUserDetailsService;
import com.eduze.manage.auth.service.AuthService;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.platform.runtime.PlatformException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class RoleSwitchServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BranchAccessGuard guard = mock(BranchAccessGuard.class);
    private final CustomUserDetailsService users = mock(CustomUserDetailsService.class);
    private final AuthService auth = mock(AuthService.class);
    private final RoleSwitchService service = new RoleSwitchService(jdbc, guard, users, auth);

    private CustomUserDetails user(Set<String> roles, Set<String> permissions) {
        return new CustomUserDetails(
                1L, 1L, "staff", "员工", "hash", List.of(2L), roles, permissions, 1, true);
    }

    @Test
    void parentCanSwitchBackToVerifiedCustomEmployeeAccount() {
        when(guard.requireUser()).thenReturn(user(Set.of("PARENT"), Set.of()));
        var employee = user(Set.of("STUDIO_ASSISTANT"), Set.of("student:read"));
        when(users.loadByUserId(1L)).thenReturn(employee);
        var result = LoginResponse.builder().accessToken("staff-token").build();
        when(auth.issueTokens(employee, "STAFF")).thenReturn(result);
        assertSame(result, service.switchRole("STAFF"));
        verify(auth).issueTokens(employee, "STAFF");
    }

    @Test
    void parentVisitorAndRolelessAccountsCannotSwitchIntoStaff() {
        when(guard.requireUser()).thenReturn(user(Set.of("PARENT"), Set.of()));
        for (Set<String> roles :
                List.of(Set.of("PARENT", "TEACHER"), Set.of("VISITOR"), Set.<String>of())) {
            when(users.loadByUserId(1L)).thenReturn(user(roles, Set.of("student:read")));
            assertThrows(PlatformException.class, () -> service.switchRole("STAFF"));
        }
        verifyNoInteractions(auth);
    }

    @Test
    void parentSwitchAlwaysRequestsParentScopedTokens() {
        var employee = user(Set.of("STUDIO_ASSISTANT"), Set.of("student:read"));
        when(guard.requireUser()).thenReturn(employee);
        when(users.loadByUserId(1L)).thenReturn(employee);
        service.switchRole("PARENT");
        verify(auth).issueTokens(employee, "PARENT");
    }

    @Test
    void parentActiveSessionStripsLiveCustomEmployeePermissionsAndBranches() {
        var jwt = mock(com.eduze.manage.auth.service.JwtService.class);
        var blacklist = mock(com.eduze.manage.auth.service.JwtBlacklistService.class);
        var claims = mock(io.jsonwebtoken.Claims.class);
        var properties =
                new com.eduze.manage.common.config.AppProperties(
                        new org.springframework.mock.env.MockEnvironment());
        properties.getTenant().setDefaultId(1L);
        when(jwt.parse("parent-token")).thenReturn(claims);
        when(claims.get(com.eduze.manage.auth.service.JwtService.CLAIM_TYPE, String.class))
                .thenReturn("access");
        when(claims.get(com.eduze.manage.auth.service.JwtService.CLAIM_TENANT)).thenReturn(1L);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get(
                        com.eduze.manage.auth.service.JwtService.CLAIM_TOKEN_VERSION,
                        Integer.class))
                .thenReturn(1);
        when(claims.get(com.eduze.manage.auth.service.JwtService.CLAIM_ACTIVE_ROLE, String.class))
                .thenReturn("PARENT");
        when(users.loadByUserId(1L))
                .thenReturn(user(Set.of("STUDIO_ASSISTANT"), Set.of("student:read")));
        try {
            var actor =
                    new SessionIntrospection(jwt, blacklist, users, properties)
                            .introspect("parent-token");
            assertEquals(Set.of("PARENT"), actor.roles());
            assertTrue(actor.permissions().isEmpty());
            assertTrue(actor.branchIds().isEmpty());
            assertFalse(actor.isStaff());
            assertThrows(PlatformException.class, () -> actor.requirePermission("student:read"));
        } finally {
            com.eduze.manage.tenant.TenantContext.clear();
        }
    }
}
