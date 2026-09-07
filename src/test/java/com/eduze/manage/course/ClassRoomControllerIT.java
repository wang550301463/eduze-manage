package com.eduze.manage.course;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ClassRoomControllerIT extends AbstractApiIT {

    @Test
    void crudClassRoom() throws Exception {
        String token = adminToken();

        var create = mockMvc.perform(post("/api/class-rooms")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":1,\"name\":\"一号画室\",\"capacity\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("一号画室"))
                .andReturn();

        long roomId = objectMapper
                .readTree(create.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        mockMvc.perform(delete("/api/class-rooms/" + roomId).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
