package com.eduze.manage.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.AbstractITContainerTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class MeControllerIT extends AbstractITContainerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void changePassword_withValidOldPassword_succeeds() throws Exception {
        String token = loginToken("admin", "admin@123");
        mockMvc.perform(
                        post("/api/me/password")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"oldPassword":"admin@123","newPassword":"admin@456"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"username":"admin","password":"admin@456"}
                                """))
                .andExpect(status().isOk());

        // restore password for other tests
        String newToken = loginToken("admin", "admin@456");
        mockMvc.perform(
                        post("/api/me/password")
                                .header("Authorization", "Bearer " + newToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"oldPassword":"admin@456","newPassword":"admin@123"}
                                """))
                .andExpect(status().isOk());
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\""
                                                        + username
                                                        + "\",\"password\":\""
                                                        + password
                                                        + "\"}"))
                        .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("accessToken").asText();
    }
}
