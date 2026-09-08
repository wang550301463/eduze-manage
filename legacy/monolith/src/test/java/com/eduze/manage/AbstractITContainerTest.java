package com.eduze.manage;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

/**
 * Shared MySQL + Redis for all *IT classes in one JVM. Manual start avoids per-class
 * {@code @Container} lifecycle churn that caused connection timeouts when running the full
 * integration suite.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestMapperConfig.class)
public abstract class AbstractITContainerTest {

    private static final MySQLContainer<?> MYSQL;
    private static final GenericContainer<?> REDIS;

    static {
        MYSQL =
                new MySQLContainer<>("mysql:8.0.36")
                        .withDatabaseName("eduze")
                        .withUsername("test")
                        .withPassword("test")
                        .withCommand(
                                "--character-set-server=utf8mb4",
                                "--collation-server=utf8mb4_unicode_ci");
        REDIS = new GenericContainer<>("redis:7.2-alpine").withExposedPorts(6379);
        MYSQL.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () ->
                        MYSQL.getJdbcUrl()
                                + "?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add(
                "spring.datasource.hikari.connection-init-sql",
                () -> "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
    }
}
