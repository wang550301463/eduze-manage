package com.eduze.manage.integration;

import com.eduze.platform.runtime.PlatformException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RosterCapacity {
    private final JdbcTemplate jdbc;

    /** Every per-session admission takes this lock before counting, including staff additions. */
    public Map<String, Object> lock(String tenant, String session) {
        var rows =
                jdbc.queryForList(
                        "SELECT id,branch_id,class_group_id,teacher_availability_id,teacher_id,start_at,status FROM t_lesson WHERE tenant_id=? AND id=? AND deleted_at=0 FOR UPDATE",
                        tenant,
                        session);
        if (rows.isEmpty()) throw new PlatformException(404, "课次不存在");
        return rows.get(0);
    }

    public void requireSpace(String tenant, String session, Map<String, Object> lesson) {
        Integer capacity = null;
        if (lesson.get("teacher_availability_id") != null)
            capacity =
                    jdbc.queryForObject(
                            "SELECT capacity FROM t_teacher_availability WHERE tenant_id=? AND id=? AND deleted_at=0",
                            Integer.class,
                            tenant,
                            lesson.get("teacher_availability_id"));
        if (capacity == null && lesson.get("class_group_id") != null)
            capacity =
                    jdbc.queryForObject(
                            "SELECT capacity FROM t_class_group WHERE tenant_id=? AND id=? AND deleted_at=0",
                            Integer.class,
                            tenant,
                            lesson.get("class_group_id"));
        if (capacity == null) throw new PlatformException(409, "课次尚未设置可用名额");
        Integer used =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_lesson_student WHERE tenant_id=? AND lesson_id=? AND status='BOOKED' AND deleted_at=0",
                        Integer.class,
                        tenant,
                        session);
        if (used >= capacity) throw new PlatformException(409, "课次名额已满");
    }
}
