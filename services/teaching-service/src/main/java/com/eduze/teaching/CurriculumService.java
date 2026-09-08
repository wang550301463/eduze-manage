package com.eduze.teaching;

import com.eduze.platform.runtime.Actor;
import com.eduze.platform.runtime.PlatformException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CurriculumService {
    private final JdbcTemplate jdbc;

    public CurriculumService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> stages(Actor actor) {
        List<Map<String, Object>> rows =
                jdbc.query(
                        "SELECT id,code,name,age_min,age_max,order_no,lorenfield_phase,description FROM t_curriculum_stage WHERE tenant_id=? AND deleted_at=0 ORDER BY order_no",
                        (rs, n) -> {
                            Map<String, Object> value = new LinkedHashMap<>();
                            value.put("id", rs.getString("id"));
                            value.put("code", rs.getString("code"));
                            value.put("name", rs.getString("name"));
                            value.put("ageMin", rs.getInt("age_min"));
                            value.put("ageMax", rs.getInt("age_max"));
                            value.put("orderNo", rs.getInt("order_no"));
                            value.put("lorenfieldPhase", rs.getString("lorenfield_phase"));
                            value.put("description", rs.getString("description"));
                            return value;
                        },
                        actor.tenantId());
        List<Map<String, Object>> mappings =
                jdbc.query(
                        "SELECT s.stage_id,d.id,d.code,d.name,d.kind,s.weight FROM t_stage_dimension s JOIN t_curriculum_dimension d ON d.id=s.dimension_id AND d.tenant_id=s.tenant_id WHERE s.tenant_id=? AND d.deleted_at=0 ORDER BY d.order_no",
                        (rs, n) ->
                                Map.of(
                                        "stageId",
                                        rs.getString("stage_id"),
                                        "id",
                                        rs.getString("id"),
                                        "code",
                                        rs.getString("code"),
                                        "name",
                                        rs.getString("name"),
                                        "kind",
                                        rs.getString("kind"),
                                        "weight",
                                        rs.getInt("weight")),
                        actor.tenantId());
        for (Map<String, Object> row : rows) {
            row.put(
                    "dimensions",
                    mappings.stream()
                            .filter(m -> m.get("stageId").equals(row.get("id")))
                            .map(
                                    m -> {
                                        Map<String, Object> copy = new LinkedHashMap<>(m);
                                        copy.remove("stageId");
                                        return copy;
                                    })
                            .toList());
        }
        return rows;
    }

    public Map<String, Object> stage(Actor actor, String id) {
        return stages(actor).stream()
                .filter(s -> id.equals(s.get("id")))
                .findFirst()
                .orElseThrow(() -> new PlatformException(404, "阶段不存在"));
    }

    public List<Map<String, Object>> dimensions(Actor actor, String kind) {
        return jdbc
                .query(
                        "SELECT id,kind,code,name,description,order_no FROM t_curriculum_dimension WHERE tenant_id=? AND deleted_at=0 ORDER BY kind,order_no",
                        (rs, n) -> {
                            Map<String, Object> value = new LinkedHashMap<>();
                            value.put("id", rs.getString("id"));
                            value.put("kind", rs.getString("kind"));
                            value.put("code", rs.getString("code"));
                            value.put("name", rs.getString("name"));
                            value.put("description", rs.getString("description"));
                            value.put("orderNo", rs.getInt("order_no"));
                            return value;
                        },
                        actor.tenantId())
                .stream()
                .filter(d -> kind == null || kind.isBlank() || kind.equals(d.get("kind")))
                .toList();
    }
}
