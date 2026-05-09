package com.eduze.manage.common.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
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

    private final Jwt jwt = new Jwt();
    private final Tenant tenant = new Tenant();
    private final Storage storage = new Storage();

    private final Environment environment;

    public AppProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateProdSecrets() {
        if (Arrays.stream(environment.getActiveProfiles()).noneMatch("prod"::equals)) {
            return;
        }
        String dbPassword = environment.getProperty("spring.datasource.password");
        if (dbPassword == null || dbPassword.isBlank()) {
            throw new IllegalStateException(
                    "spring.datasource.password (DB_PASSWORD) must be non-blank when profile 'prod' is active");
        }
        String redisPassword = environment.getProperty("spring.data.redis.password");
        if (redisPassword == null || redisPassword.isBlank()) {
            throw new IllegalStateException(
                    "spring.data.redis.password (REDIS_PASSWORD) must be non-blank when profile 'prod' is active");
        }
        String secret = jwt.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "eduze.jwt.secret (JWT_SECRET) must be non-blank when profile 'prod' is active");
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
}
