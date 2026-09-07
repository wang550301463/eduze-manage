package com.eduze.manage.course;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class ClassGroupMemberIT extends AbstractApiIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;
    private long classGroupId;

    @BeforeEach
    void seed() throws Exception {
        token = adminToken();
        jdbcTemplate.update(
                "INSERT INTO t_student (id, tenant_id, branch_id, enroll_no, name, status, created_at, updated_at, deleted_at, version) VALUES (9001,1,1,'E9001','学员甲',1,NOW(3),NOW(3),0,1) ON DUPLICATE KEY UPDATE name=VALUES(name)");
        jdbcTemplate.update(
                "INSERT INTO t_student (id, tenant_id, branch_id, enroll_no, name, status, created_at, updated_at, deleted_at, version) VALUES (9002,1,1,'E9002','学员乙',1,NOW(3),NOW(3),0,1) ON DUPLICATE KEY UPDATE name=VALUES(name)");

        long courseId = objectMapper
                .readTree(mockMvc.perform(post("/api/courses")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"成员测试课\",\"lessonMinutes\":60}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();

        classGroupId = objectMapper
                .readTree(mockMvc.perform(post("/api/class-groups")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"branchId":1,"name":"容量测试班","courseId":%d,"capacity":1}
                                        """
                                                .formatted(courseId)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("data")
                .get("id")
                .asLong();
    }

    @Test
    void addMember_respectsCapacity() throws Exception {
        mockMvc.perform(post("/api/class-groups/" + classGroupId + "/members")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[9001]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/class-groups/" + classGroupId + "/members")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[9002]}"))
                .andExpect(status().isConflict());
    }
}
