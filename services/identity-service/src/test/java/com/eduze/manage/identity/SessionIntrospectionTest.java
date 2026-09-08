package com.eduze.manage.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.manage.auth.security.*;
import com.eduze.manage.auth.service.*;
import com.eduze.manage.common.config.AppProperties;
import com.eduze.manage.tenant.TenantContext;
import com.eduze.platform.runtime.PlatformException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.*;

class SessionIntrospectionTest {
    private JwtService jwt;
    private JwtBlacklistService blacklist;
    private CustomUserDetailsService users;
    private SessionIntrospection sessions;

    @BeforeEach
    void setup() {
        var props = new AppProperties(null);
        props.getJwt().setSecret("test-jwt-secret-minimum-32-chars-long");
        props.getTenant().setDefaultId(1L);
        jwt = new JwtService(props);
        blacklist = mock(JwtBlacklistService.class);
        users = mock(CustomUserDetailsService.class);
        sessions = new SessionIntrospection(jwt, blacklist, users, props);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private String token(int version) {
        return jwt.generateAccess(
                7L,
                "test-jti",
                1L,
                version,
                List.of(1L, 2L),
                List.of("SUPER_ADMIN"),
                List.of("user:write"));
    }

    private CustomUserDetails user(int version, boolean enabled) {
        return new CustomUserDetails(
                7L,
                1L,
                "teacher",
                "老师",
                null,
                List.of(2L),
                Set.of("TEACHER"),
                List.of("student:read"),
                version,
                enabled);
    }

    @Test
    void currentPermissionsReplaceStaleSignedClaims() {
        when(users.loadByUserId(7L)).thenReturn(user(1, true));
        var actor = sessions.introspect(token(1));
        assertEquals(Set.of("2"), actor.branchIds());
        assertEquals(Set.of("TEACHER"), actor.roles());
        assertFalse(actor.permissions().contains("user:write"));
    }

    @Test
    void revokedVersionAndDisabledAccountsAreRejected() {
        when(users.loadByUserId(7L)).thenReturn(user(2, true));
        assertEquals(
                401,
                assertThrows(PlatformException.class, () -> sessions.introspect(token(1)))
                        .getStatus());
        when(users.loadByUserId(7L)).thenReturn(user(1, false));
        assertThrows(PlatformException.class, () -> sessions.introspect(token(1)));
    }

    @Test
    void parentModeCannotInheritStaffPrivileges() {
        when(users.loadByUserId(7L)).thenReturn(user(1, true));
        String token =
                jwt.generateScoped(
                        7L,
                        "parent",
                        1L,
                        1,
                        List.of(2L),
                        List.of("TEACHER"),
                        List.of("student:write"),
                        JwtService.TYPE_ACCESS,
                        "PARENT");
        var actor = sessions.introspect(token);
        assertEquals(Set.of("PARENT"), actor.roles());
        assertFalse(actor.isStaff());
        assertTrue(actor.permissions().isEmpty());
        assertTrue(actor.branchIds().isEmpty());
    }

    @Test
    void blacklistRefreshTokenAndWrongTenantAreRejected() {
        when(blacklist.isBlacklisted("test-jti")).thenReturn(true);
        assertThrows(PlatformException.class, () -> sessions.introspect(token(1)));
        assertThrows(
                PlatformException.class,
                () ->
                        sessions.introspect(
                                jwt.generateRefresh(
                                        7L, "refresh", 1L, 1, List.of(), List.of(), List.of())));
        assertThrows(
                PlatformException.class,
                () ->
                        sessions.introspect(
                                jwt.generateAccess(
                                        7L, "foreign", 2L, 1, List.of(), List.of(), List.of())));
        verifyNoInteractions(users);
    }
}
