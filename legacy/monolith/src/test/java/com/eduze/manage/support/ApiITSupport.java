package com.eduze.manage.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Component
public class ApiITSupport {

    @Autowired private ObjectProvider<MockMvc> mockMvcProvider;

    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc() {
        MockMvc mvc = mockMvcProvider.getIfAvailable();
        if (mvc == null) {
            throw new IllegalStateException(
                    "MockMvc is not available; add @AutoConfigureMockMvc on the test (extend AbstractApiIT)");
        }
        return mvc;
    }

    public String login(String username, String password) throws Exception {
        MvcResult result =
                mockMvc()
                        .perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\""
                                                        + username
                                                        + "\",\"password\":\""
                                                        + password
                                                        + "\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("data").get("accessToken").asText();
    }

    public RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }
}
