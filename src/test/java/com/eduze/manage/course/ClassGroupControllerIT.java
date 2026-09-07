package com.eduze.manage.course;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ClassGroupControllerIT extends AbstractApiIT {

    private static final AtomicInteger SLOT = new AtomicInteger(100);

    @Test
    void crudClassGroup() throws Exception {
        String token = adminToken();
        int start = SLOT.getAndAdd(90);

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
                                {
                                  "branchId":1,
                                  "name":"周六上午A班",
                                  "courseId":%d,
                                  "nestedAvailability":{
                                    "teacherId":1101,
                                    "branchId":1,
                                    "dayOfWeek":1,
                                    "startMinute":%d,
                                    "endMinute":%d,
                                    "capacity":10,
                                    "validFrom":"2026-01-01",
                                    "status":1
                                  }
                                }
                                """
                                        .formatted(courseId, start, start + 60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentCount").value(0))
                .andExpect(jsonPath("$.data.teacherAvailabilityId").isNotEmpty())
                .andExpect(jsonPath("$.data.dayOfWeek").value(1))
                .andExpect(jsonPath("$.data.capacity").value(10))
                .andReturn();

        long groupId = objectMapper
                .readTree(create.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        mockMvc.perform(delete("/api/class-groups/" + groupId).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void create_duplicateAvailabilityBind_conflict() throws Exception {
        String token = adminToken();
        int start = SLOT.getAndAdd(90);

        long availId = objectMapper
                .readTree(mockMvc.perform(post("/api/teachers/1101/availabilities")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "branchId":1,
                                          "dayOfWeek":2,
                                          "startMinute":%d,
                                          "endMinute":%d,
                                          "capacity":8,
                                          "validFrom":"2026-01-01",
                                          "status":1
                                        }
                                        """
                                                .formatted(start, start + 60)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        long courseId = objectMapper
                .readTree(mockMvc.perform(post("/api/courses")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"重复绑定课\",\"lessonMinutes\":60}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        mockMvc.perform(post("/api/class-groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"branchId":1,"name":"组A","courseId":%d,"teacherAvailabilityId":%d}
                                """
                                        .formatted(courseId, availId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/class-groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"branchId":1,"name":"组B","courseId":%d,"teacherAvailabilityId":%d}
                                """
                                        .formatted(courseId, availId)))
                .andExpect(status().isConflict());
    }
}
