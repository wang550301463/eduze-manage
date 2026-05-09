package com.eduze.manage.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SpaFallbackTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deepLinkWithoutExtension_returnsIndexHtml() throws Exception {
        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<!doctype html>")));
    }

    @Test
    void unknownApiPath_returns404() throws Exception {
        mockMvc.perform(get("/api/__not_exist__")).andExpect(status().isNotFound());
    }
}
