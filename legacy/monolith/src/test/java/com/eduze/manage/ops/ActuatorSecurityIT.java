package com.eduze.manage.ops;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.support.ApiITSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ActuatorSecurityIT extends AbstractITContainerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ApiITSupport api;

    @Test
    void health_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void prometheus_requiresAuth() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        String token = api.login("admin", "admin@123");
        mockMvc.perform(get("/actuator/prometheus").with(api.bearer(token)))
                .andExpect(status().isOk());
    }
}
