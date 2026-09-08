package com.eduze.manage.identity;

import static org.junit.jupiter.api.Assertions.*;

import com.eduze.manage.IdentityApplication;
import com.eduze.manage.auth.service.JwtBlacklistService;
import com.eduze.manage.auth.service.JwtService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
@SpringBootTest(
        classes = IdentityApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "eduze.jwt.secret=test-jwt-secret-minimum-32-chars-long",
                    "eduze.runtime.service-name=identity",
            "eduze.runtime.service-token=test-identity-service-token-123456",
                    "eduze.runtime.callers.academic=test-academic-service-token-123456",
            "spring.data.redis.password=test-password",
                    "eduze.security.login-rate-limit-enabled=false"
        })
class IdentityDatabaseTest {
    @Container
    static org.testcontainers.containers.GenericContainer<?> redis =
            new org.testcontainers.containers.GenericContainer<>("redis:7.2-alpine")
                    .withExposedPorts(6379)
                    .withCommand("redis-server", "--requirepass", "test-password");

    @Container static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired TestRestTemplate http;
    @Autowired JwtService jwt;
    @MockitoBean JwtBlacklistService blacklist;

    @Test
    void independentSchemaAndInternalCredentialBoundary() {
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='t_student'",
                        Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM t_user", Integer.class));
        assertEquals(
                401,
                http.postForEntity(
                                "/internal/identity/introspect",
                                Map.of("token", "bad"),
                                String.class)
                        .getStatusCode()
                        .value());
        jdbc.update(
                "INSERT INTO t_user(id,tenant_id,username,password_hash,name,status,token_version) VALUES(7,1,'teacher','unusable','老师',1,1)");
        jdbc.update("INSERT INTO t_user_branch(id,tenant_id,user_id,branch_id) VALUES(7,1,7,1)");
        jdbc.update("INSERT INTO t_user_role(id,tenant_id,user_id,role_id) VALUES(7,1,7,4)");
        String token =
                jwt.generateAccess(
                        7L,
                        "live",
                        1L,
                        1,
                        List.of(1L),
                        List.of("SUPER_ADMIN"),
                        List.of("user:write"));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-Name", "academic");
        headers.set("X-Service-Token", "test-academic-service-token-123456");
        var response =
                http.postForEntity(
                        "/internal/identity/introspect",
                        new HttpEntity<>(Map.of("token", token), headers),
                        Map.class);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("TEACHER"), response.getBody().get("roles"));
        jdbc.update("UPDATE t_user SET token_version=2 WHERE id=7");
        assertEquals(
                401,
                http.postForEntity(
                                "/internal/identity/introspect",
                                new HttpEntity<>(Map.of("token", token), headers),
                                String.class)
                        .getStatusCode()
                        .value());
    }
}
