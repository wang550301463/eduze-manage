package com.eduze.manage.course;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class CourseControllerIT extends AbstractApiIT {

    @Test
    void crudCourse() throws Exception {
        String token = adminToken();

        MvcResult create = mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"启蒙绘画","ageMin":4,"ageMax":6,"lessonMinutes":90}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("启蒙绘画"))
                .andReturn();

        long id = objectMapper
                .readTree(create.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        mockMvc.perform(get("/api/courses/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lessonMinutes").value(90));

        mockMvc.perform(put("/api/courses/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"启蒙绘画进阶","ageMin":4,"ageMax":6,"lessonMinutes":90}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("启蒙绘画进阶"));

        mockMvc.perform(delete("/api/courses/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
