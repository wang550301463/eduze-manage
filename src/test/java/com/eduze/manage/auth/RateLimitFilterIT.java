package com.eduze.manage.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.common.config.AppProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class RateLimitFilterIT extends AbstractITContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AppProperties appProperties;

    private boolean previousRateLimitEnabled;

    @BeforeEach
    void enableRateLimit() {
        previousRateLimitEnabled = appProperties.getSecurity().isLoginRateLimitEnabled();
        appProperties.getSecurity().setLoginRateLimitEnabled(true);
        clearRateLimitKeys();
    }

    @AfterEach
    void restoreRateLimit() {
        appProperties.getSecurity().setLoginRateLimitEnabled(previousRateLimitEnabled);
        clearRateLimitKeys();
    }

    void clearRateLimitKeys() {
        var keys = redisTemplate.keys("ratelimit:login:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void moreThan60RequestsPerMinute_returns429() throws Exception {
        String body =
                """
                {"username":"admin","password":"admin@123"}
                """;
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .header("X-Forwarded-For", "203.0.113.99"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Forwarded-For", "203.0.113.99"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(42900));
    }
}
