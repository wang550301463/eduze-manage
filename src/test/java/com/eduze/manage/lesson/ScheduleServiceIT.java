package com.eduze.manage.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ScheduleServiceIT extends AbstractApiIT {

    @Test
    void weekSchedule_returnsSevenDays() throws Exception {
        String token = adminToken();
        long courseId = objectMapper
                .readTree(mockMvc.perform(post("/api/courses")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"周课表课\",\"lessonMinutes\":60}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        long groupId = objectMapper
                .readTree(mockMvc.perform(post("/api/class-groups")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"branchId":1,"name":"周课表班","courseId":%d,"capacity":10}
                                        """
                                                .formatted(courseId)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/lessons")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    """
                                    {
                                      "branchId": 1,
                                      "classGroupId": %d,
                                      "startAt": "2026-05-1%dT09:00:00",
                                      "endAt": "2026-05-1%dT10:00:00"
                                    }
                                    """
                                            .formatted(groupId, i, i)))
                    .andExpect(status().isOk());
        }

        var week = objectMapper.readTree(mockMvc.perform(get("/api/schedule/week")
                        .header("Authorization", bearer(token))
                        .param("branchId", "1")
                        .param("weekStart", "2026-05-11"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertEquals(7, week.get("data").get("days").size());
    }
}
