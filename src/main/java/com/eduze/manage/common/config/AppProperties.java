package com.eduze.manage.common.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "eduze")
@Validated
@Getter
public class AppProperties {

    private static final Set<String> WEAK_JWT_SECRETS = Set.of(
            "change-me-jwt-secret-at-least-32-chars",
            "dev-only-secret-change-me-in-prod-32bytes-min",
            "test-jwt-secret-minimum-32-chars-long");

    private final Jwt jwt = new Jwt();
    private final Tenant tenant = new Tenant();
    private final Storage storage = new Storage();
    private final Security security = new Security();
    private final Bootstrap bootstrap = new Bootstrap();

    private final Environment environment;

    public AppProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateProdSecrets() {
        if (Arrays.stream(environment.getActiveProfiles()).noneMatch("prod"::equals)) {
            return;
        }
        requireNonBlank(jwt.getSecret(), "eduze.jwt.secret (JWT_SECRET)");
        if (jwt.getSecret().length() < 32) {
            throw new IllegalStateException("eduze.jwt.secret (JWT_SECRET) must be at least 32 characters in prod");
        }
        String normalized = jwt.getSecret().trim().toLowerCase(Locale.ROOT);
        if (WEAK_JWT_SECRETS.contains(normalized) || normalized.contains("change-me")) {
            throw new IllegalStateException("eduze.jwt.secret (JWT_SECRET) must not use a known weak/default value");
        }
        requireNonBlank(environment.getProperty("spring.datasource.password"), "spring.datasource.password (DB_PASSWORD)");
        requireNonBlank(environment.getProperty("spring.data.redis.password"), "spring.data.redis.password (REDIS_PASSWORD)");
        String dbPassword = environment.getProperty("spring.datasource.password");
        if (dbPassword != null && dbPassword.length() < 8) {
            throw new IllegalStateException("spring.datasource.password (DB_PASSWORD) must be at least 8 characters in prod");
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be non-blank when profile 'prod' is active");
        }
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private Integer accessTtlMin;
        private Integer refreshTtlDay;
    }

    @Getter
    @Setter
    public static class Tenant {
        private Long defaultId;
    }

    @Getter
    @Setter
    public static class Storage {
        private String localRoot;
    }

    @Getter
    @Setter
    public static class Security {
        /** Login IP rate limit (POST /api/auth/login). Disabled in test profile by default. */
        private boolean loginRateLimitEnabled = true;
    }

    @Getter
    @Setter
    public static class Bootstrap {
        /** One-time prod bootstrap admin username from BOOTSTRAP_ADMIN_USERNAME. */
        private String adminUsername;
        /** One-time prod bootstrap admin password from BOOTSTRAP_ADMIN_PASSWORD. */
        private String adminPassword;
    }
}
