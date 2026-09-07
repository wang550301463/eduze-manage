package com.eduze.manage.course;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ClassGroupControllerIT extends AbstractApiIT {

    @Test
    void crudClassGroup() throws Exception {
        String token = adminToken();

        long courseId = objectMapper
                .readTree(mockMvc.perform(post("/api/courses")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"周六班课程\",\"lessonMinutes\":60}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        var create = mockMvc.perform(post("/api/class-groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"branchId":1,"name":"周六上午A班","courseId":%d,"capacity":10,"headTeacherId":1001}
                                """
                                        .formatted(courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentCount").value(0))
                .andReturn();

        long groupId = objectMapper
                .readTree(create.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        mockMvc.perform(delete("/api/class-groups/" + groupId).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
