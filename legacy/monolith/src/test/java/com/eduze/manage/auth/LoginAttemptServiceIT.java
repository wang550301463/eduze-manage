package com.eduze.manage.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.auth.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class LoginAttemptServiceIT extends AbstractITContainerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private LoginAttemptService loginAttemptService;

    @BeforeEach
    void reset() {
        loginAttemptService.reset("lock-test-user");
    }

    @Test
    void afterFiveFailures_sixthReturns423() throws Exception {
        String body =
                """
                {"username":"lock-test-user","password":"wrong"}
                """;
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value(42301));
    }
}
