package com.eduze.manage.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.eduze.manage.auth.service.JwtService;
import com.eduze.manage.common.config.AppProperties;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(null);
        AppProperties.Jwt jwt = props.getJwt();
        jwt.setSecret("test-jwt-secret-minimum-32-chars-long");
        jwt.setAccessTtlMin(15);
        jwt.setRefreshTtlDay(7);
        jwtService = new JwtService(props);
    }

    @Test
    void generateAndParse_returnsUserIdTenantIdAndJti() {
        String jti = jwtService.newJti();
        Long userId = 100L;
        Long tenantId = 1L;
        String token =
                jwtService.generateAccess(
                        userId,
                        jti,
                        tenantId,
                        3,
                        List.of(1L),
                        List.of("SUPER_ADMIN"),
                        List.of("user:read"));

        Claims claims = jwtService.parse(token);
        assertEquals(jti, claims.getId());
        assertEquals(String.valueOf(userId), claims.getSubject());
        assertEquals(tenantId, claims.get(JwtService.CLAIM_TENANT, Long.class));
        assertEquals(3, claims.get(JwtService.CLAIM_TOKEN_VERSION, Integer.class));
        assertNotNull(claims.getExpiration());
    }
}
