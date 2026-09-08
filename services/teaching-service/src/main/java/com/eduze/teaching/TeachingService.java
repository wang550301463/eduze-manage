package com.eduze.teaching;

import static com.eduze.teaching.TeachingModels.*;

import com.eduze.platform.runtime.Actor;
import com.eduze.platform.runtime.InternalClient;
import com.eduze.platform.runtime.Outbox;
import com.eduze.platform.runtime.PlatformException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Owns teaching content and plans; academic identity and lesson validation remain remote. */
@Service
public class TeachingService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final InternalClient client;
    private final Outbox outbox;
    private final TransactionTemplate transaction;

    public TeachingService(
            JdbcTemplate jdbc, ObjectMapper json, InternalClient client, Outbox outbox) {
        this.jdbc = jdbc;
        this.json = json;
        this.client = client;
        this.outbox = outbox;
        this.transaction =
                new TransactionTemplate(
                        new DataSourceTransactionManager(
                                Objects.requireNonNull(jdbc.getDataSource())));
    }

    private void staff(Actor actor, boolean write) {
        if (!actor.isStaff()) {
            throw new PlatformException(403, "需要员工权限");
        }
        if (!write || !actor.permissions().contains("teaching:write")) {
            actor.requirePermission(write ? "course:write" : "course:read");
        }
    }

    public Template createTemplate(Actor actor, TemplateInput input) {
        return createTemplateFromSource(actor, input, Set.of());
    }

    public Template copyTemplate(Actor actor, String publishedVersionId) {
        TemplateVersion source = templateVersion(actor, publishedVersionId);
        return createTemplateFromSource(
                actor, source.content(), new HashSet<>(source.content().mediaIds()));
    }

    private Template createTemplateFromSource(
            Actor actor, TemplateInput input, Set<String> sourceMedia) {
        staff(actor, true);
        validateTemplate(input);
        media(actor, input.mediaIds(), sourceMedia);
        String id = id();
        Instant now = Instant.now();
        return transaction.execute(
                tx -> {
                    jdbc.update(
                            "INSERT INTO teaching_template(id,tenant_id,owner_id,row_version,title,content_json,created_at) VALUES (?,?,?,1,?,?,?)",
                            id,
                            actor.tenantId(),
                            actor.userId(),
                            input.title(),
                            encode(input),
                            Timestamp.from(now));
                    references(actor, "template", id, input.mediaIds(), 1);
                    return new Template(id, actor.userId(), 1, input, now);
                });
    }

    public Template updateTemplate(Actor actor, String id, TemplateInput input) {
        staff(actor, true);
        Template previous = template(actor, id);
        validateTemplate(input);
        media(actor, input.mediaIds(), new HashSet<>(previous.content().mediaIds()));
        return transaction.execute(
                tx -> {
                    changed(
                            jdbc.update(
                                    "UPDATE teaching_template SET title=?,content_json=?,row_version=row_version+1 WHERE id=? AND tenant_id=? AND row_version=?",
                                    input.title(),
                                    encode(input),
                                    id,
                                    actor.tenantId(),
                                    input.version()));
                    references(actor, "template", id, input.mediaIds(), input.version() + 1);
                    return template(actor, id);
                });
    }

    public Template template(Actor actor, String id) {
        staff(actor, false);
        Template result =
                one(
                        jdbc.query(
                                "SELECT id,owner_id,row_version,content_json,created_at FROM teaching_template WHERE id=? AND tenant_id=?",
                                this::templateRow,
                                id,
                                actor.tenantId()));
        if (!result.ownerId().equals(actor.userId()) && !actor.isSuperAdmin()) {
            throw new PlatformException(403, "无权读取他人草稿，需要所有者权限");
        }
        return result;
    }

    public List<Template> templates(Actor actor, String q, int limit) {
        staff(actor, false);
        return jdbc.query(
                "SELECT id,owner_id,row_version,content_json,created_at FROM teaching_template WHERE tenant_id=? AND owner_id=? AND content_json LIKE ? ORDER BY created_at DESC LIMIT ?",
                this::templateRow,
                actor.tenantId(),
                actor.userId(),
                search(q),
                limit(limit));
    }

    public TemplateVersion publishTemplate(Actor actor, String id, int version) {
        staff(actor, true);
        Template preflight = template(actor, id);
        checkVersion(preflight.version(), version);
        media(actor, preflight.content().mediaIds(), new HashSet<>(preflight.content().mediaIds()));
        return transaction.execute(
                tx -> {
                    jdbc.queryForObject(
                            "SELECT row_version FROM teaching_template WHERE id=? AND tenant_id=? FOR UPDATE",
                            Integer.class,
                            id,
                            actor.tenantId());
                    Template draft = template(actor, id);
                    checkVersion(draft.version(), version);
                    List<TemplateVersion> existing =
                            jdbc.query(
                                    "SELECT id,template_id,version_number,content_json,created_at FROM teaching_template_version WHERE template_id=? AND tenant_id=? AND version_number=?",
                                    this::versionRow,
                                    id,
                                    actor.tenantId(),
                                    version);
                    if (!existing.isEmpty()) {
                        return existing.get(0);
                    }
                    String versionId = id();
                    Instant now = Instant.now();
                    jdbc.update(
                            "INSERT INTO teaching_template_version(id,tenant_id,template_id,version_number,title,content_json,created_at) VALUES (?,?,?,?,?,?,?)",
                            versionId,
                            actor.tenantId(),
                            id,
                            version,
                            draft.content().title(),
                            encode(draft.content()),
                            Timestamp.from(now));
                    references(actor, "template-version", versionId, draft.content().mediaIds(), 1);
                    return new TemplateVersion(versionId, id, version, draft.content(), now);
                });
    }

    public TemplateVersion templateVersion(Actor actor, String id) {
        staff(actor, false);
        return one(
                jdbc.query(
                        "SELECT id,template_id,version_number,content_json,created_at FROM teaching_template_version WHERE id=? AND tenant_id=?",
                        this::versionRow,
                        id,
                        actor.tenantId()));
    }

    public List<TemplateVersion> versions(Actor actor, String templateId, String q, int limit) {
        staff(actor, false);
        if (templateId != null) {
            return jdbc.query(
                    "SELECT id,template_id,version_number,content_json,created_at FROM teaching_template_version WHERE tenant_id=? AND template_id=? ORDER BY version_number DESC LIMIT ?",
                    this::versionRow,
                    actor.tenantId(),
                    templateId,
                    limit(limit));
        }
        return jdbc.query(
                "SELECT id,template_id,version_number,content_json,created_at FROM teaching_template_version WHERE tenant_id=? AND content_json LIKE ? ORDER BY created_at DESC LIMIT ?",
                this::versionRow,
                actor.tenantId(),
                search(q),
                limit(limit));
    }

    public Resource createResource(Actor actor, ResourceInput input) {
        return createResourceFromSource(actor, input, Set.of());
    }

    public Resource copyResource(Actor actor, String resourceId) {
        Resource source = resource(actor, resourceId);
        return createResourceFromSource(
                actor, source.content(), new HashSet<>(source.content().mediaIds()));
    }

    private Resource createResourceFromSource(
            Actor actor, ResourceInput input, Set<String> sourceMedia) {
        staff(actor, true);
        media(actor, input.mediaIds(), sourceMedia);
        String id = id();
        return transaction.execute(
                tx -> {
                    jdbc.update(
                            "INSERT INTO teaching_resource(id,tenant_id,owner_id,row_version,published,title,content_json,created_at) VALUES (?,?,?,1,FALSE,?,?,?)",
                            id,
                            actor.tenantId(),
                            actor.userId(),
                            input.title(),
                            encode(input),
                            Timestamp.from(Instant.now()));
                    references(actor, "resource", id, input.mediaIds(), 1);
                    return resource(actor, id);
                });
    }

    public Resource resource(Actor actor, String id) {
        staff(actor, false);
        Resource value =
                one(
                        jdbc.query(
                                "SELECT id,owner_id,row_version,published,content_json,created_at FROM teaching_resource WHERE id=? AND tenant_id=?",
                                this::resourceRow,
                                id,
                                actor.tenantId()));
        if (!value.published()
                && !value.ownerId().equals(actor.userId())
                && !actor.isSuperAdmin()) {
            throw new PlatformException(403, "无草稿权限");
        }
        return value;
    }

    public List<Resource> resources(Actor actor, String q, int limit) {
        staff(actor, false);
        return jdbc.query(
                "SELECT id,owner_id,row_version,published,content_json,created_at FROM teaching_resource WHERE tenant_id=? AND (owner_id=? OR published=TRUE) AND content_json LIKE ? ORDER BY created_at DESC LIMIT ?",
                this::resourceRow,
                actor.tenantId(),
                actor.userId(),
                search(q),
                limit(limit));
    }

    public Resource updateResource(Actor actor, String id, ResourceInput input) {
        staff(actor, true);
        Resource resource = resource(actor, id);
        if (resource.published() || !resource.ownerId().equals(actor.userId())) {
            throw new PlatformException(409, "已发布版本不可编辑，请复制新建");
        }
        media(actor, input.mediaIds(), new HashSet<>(resource.content().mediaIds()));
        return transaction.execute(
                tx -> {
                    changed(
                            jdbc.update(
                                    "UPDATE teaching_resource SET title=?,content_json=?,row_version=row_version+1 WHERE tenant_id=? AND id=? AND row_version=? AND published=FALSE",
                                    input.title(),
                                    encode(input),
                                    actor.tenantId(),
                                    id,
                                    input.version()));
                    references(actor, "resource", id, input.mediaIds(), input.version() + 1);
                    return resource(actor, id);
                });
    }

    public Resource publishResource(Actor actor, String id, int version) {
        staff(actor, true);
        Resource resource = resource(actor, id);
        if (!resource.ownerId().equals(actor.userId()) && !actor.isSuperAdmin()) {
            throw new PlatformException(403, "无发布权限");
        }
        media(actor, resource.content().mediaIds(), new HashSet<>(resource.content().mediaIds()));
        return transaction.execute(
                tx -> {
                    changed(
                            jdbc.update(
                                    "UPDATE teaching_resource SET published=TRUE,row_version=row_version+1 WHERE tenant_id=? AND id=? AND row_version=? AND published=FALSE",
                                    actor.tenantId(),
                                    id,
                                    version));
                    return resource(actor, id);
                });
    }

    @Transactional
    public Theme createTheme(Actor actor, ThemeInput input) {
        staff(actor, true);
        validateLessons(actor, input);
        TemplateVersion template = templateVersion(actor, input.templateVersionId());
        String id = id();
        Instant now = Instant.now();
        jdbc.update(
                "INSERT INTO teaching_theme(id,tenant_id,branch_id,group_id,row_version,theme_status,content_json,template_json,created_at) VALUES (?,?,?,?,1,'PLANNED',?,?,?)",
                id,
                actor.tenantId(),
                input.branchId(),
                input.groupId(),
                encode(input),
                encode(template.content()),
                Timestamp.from(now));
        audit(actor, id, "CREATE", "新建班级主题", input);
        return theme(actor, id);
    }

    public Theme theme(Actor actor, String id) {
        staff(actor, false);
        Theme result =
                one(
                        jdbc.query(
                                "SELECT id,row_version,theme_status,content_json,template_json,created_at FROM teaching_theme WHERE id=? AND tenant_id=?",
                                this::themeRow,
                                id,
                                actor.tenantId()));
        actor.requireBranch(result.content().branchId());
        Map<?, ?> access =
                client.get(
                        "academic",
                        "/internal/academic/groups/" + result.content().groupId() + "/access",
                        Map.class);
        if (access == null || !Boolean.TRUE.equals(access.get("allowed"))) {
            throw new PlatformException(403, "无班级权限");
        }
        return result;
    }

    public List<Theme> themes(Actor actor, String branch, String group, int limit) {
        staff(actor, false);
        if (branch != null && !branch.isBlank()) {
            actor.requireBranch(branch);
        }
        List<Theme> candidates =
                jdbc
                        .query(
                                "SELECT id,row_version,theme_status,content_json,template_json,created_at FROM teaching_theme WHERE tenant_id=? ORDER BY created_at DESC",
                                this::themeRow,
                                actor.tenantId())
                        .stream()
                        .filter(
                                t ->
                                        actor.isSuperAdmin()
                                                || actor.branchIds()
                                                        .contains(t.content().branchId()))
                        .filter(
                                t ->
                                        branch == null
                                                || branch.isBlank()
                                                || branch.equals(t.content().branchId()))
                        .filter(
                                t ->
                                        group == null
                                                || group.isBlank()
                                                || group.equals(t.content().groupId()))
                        .toList();
        Set<String> permitted =
                accessibleGroups(
                        candidates.stream().map(t -> t.content().groupId()).distinct().toList());
        return candidates.stream()
                .filter(t -> permitted.contains(t.content().groupId()))
                .limit(limit(limit))
                .toList();
    }

    @Transactional
    public Theme updateTheme(Actor actor, String id, ThemeInput input) {
        staff(actor, true);
        Theme previous = theme(actor, id);
        validateLessons(actor, input);
        if (!previous.content().branchId().equals(input.branchId())
                || !previous.content().groupId().equals(input.groupId())
                || !previous.content().templateVersionId().equals(input.templateVersionId())) {
            throw new PlatformException(400, "班级、校区和模板版本创建后不可替换");
        }
        changed(
                jdbc.update(
                        "UPDATE teaching_theme SET content_json=?,row_version=row_version+1 WHERE tenant_id=? AND id=? AND row_version=?",
                        encode(input),
                        actor.tenantId(),
                        id,
                        input.version()));
        audit(actor, id, "UPDATE", input.handoffNote(), input);
        return theme(actor, id);
    }

    @Transactional
    public Theme transition(Actor actor, String id, int version, String status, String reason) {
        staff(actor, true);
        Theme previous = theme(actor, id);
        if (!Set.of("PLANNED", "IN_PROGRESS", "FINISHED").contains(status)) {
            throw new PlatformException(400, "主题状态无效");
        }
        if (previous.status().equals("FINISHED")
                && !status.equals("FINISHED")
                && (reason == null || reason.isBlank())) {
            throw new PlatformException(400, "重开主题需填写原因");
        }
        changed(
                jdbc.update(
                        "UPDATE teaching_theme SET theme_status=?,row_version=row_version+1 WHERE tenant_id=? AND id=? AND row_version=?",
                        status,
                        actor.tenantId(),
                        id,
                        version));
        audit(actor, id, status, reason == null ? "" : reason, previous.content());
        return theme(actor, id);
    }

    public Object roster(Actor actor, String id) {
        Theme theme = theme(actor, id);
        return client.get(
                "academic",
                "/internal/academic/groups/" + theme.content().groupId() + "/students",
                List.class);
    }

    @Transactional
    public Plan createPlan(Actor actor, PlanInput input) {
        staff(actor, true);
        actor.requireBranch(input.branchId());
        if (input.endsOn().isBefore(input.startsOn())) {
            throw new PlatformException(400, "结束日期不能早于开始日期");
        }
        for (String themeId : input.themeIds()) {
            if (!theme(actor, themeId).content().branchId().equals(input.branchId())) {
                throw new PlatformException(400, "主题不属于计划校区");
            }
        }
        Plan plan =
                new Plan(
                        id(),
                        input.branchId(),
                        input.name(),
                        input.startsOn(),
                        input.endsOn(),
                        input.themeIds(),
                        Instant.now());
        jdbc.update(
                "INSERT INTO teaching_plan(id,tenant_id,branch_id,content_json,created_at) VALUES (?,?,?,?,?)",
                plan.id(),
                actor.tenantId(),
                plan.branchId(),
                encode(plan),
                Timestamp.from(plan.createdAt()));
        return plan;
    }

    public List<Plan> plans(Actor actor, String branch, int limit) {
        staff(actor, false);
        actor.requireBranch(branch);
        return jdbc.query(
                "SELECT content_json FROM teaching_plan WHERE tenant_id=? AND branch_id=? ORDER BY created_at DESC LIMIT ?",
                (rs, n) -> decode(rs.getString(1), Plan.class),
                actor.tenantId(),
                branch,
                limit(limit));
    }

    public Map<String, Long> dashboard(Actor actor, String branch) {
        staff(actor, false);
        if (branch != null && !branch.isBlank()) {
            actor.requireBranch(branch);
        }
        List<Map<String, Object>> counts =
                jdbc
                        .query(
                                "SELECT branch_id,group_id,theme_status,COUNT(*) AS total FROM teaching_theme WHERE tenant_id=? GROUP BY branch_id,group_id,theme_status",
                                (rs, n) ->
                                        Map.<String, Object>of(
                                                "branch",
                                                rs.getString(1),
                                                "group",
                                                rs.getString(2),
                                                "status",
                                                rs.getString(3),
                                                "total",
                                                rs.getLong(4)),
                                actor.tenantId())
                        .stream()
                        .filter(
                                r ->
                                        actor.isSuperAdmin()
                                                || actor.branchIds().contains(r.get("branch")))
                        .filter(
                                r ->
                                        branch == null
                                                || branch.isBlank()
                                                || branch.equals(r.get("branch")))
                        .toList();
        Set<String> allowed =
                accessibleGroups(
                        counts.stream().map(r -> (String) r.get("group")).distinct().toList());
        Map<String, Long> result =
                new HashMap<>(Map.of("planned", 0L, "inProgress", 0L, "finished", 0L));
        Map<String, String> keys =
                Map.of("PLANNED", "planned", "IN_PROGRESS", "inProgress", "FINISHED", "finished");
        for (Map<String, Object> row : counts) {
            if (allowed.contains(row.get("group"))) {
                result.merge(keys.get(row.get("status")), (Long) row.get("total"), Long::sum);
            }
        }
        return result;
    }

    public Object mediaAccess(Actor actor, String type, String id, List<String> ids) {
        List<String> allowed =
                switch (type) {
                    case "resources" -> resource(actor, id).content().mediaIds();
                    case "templates" -> template(actor, id).content().mediaIds();
                    case "template-versions" -> templateVersion(actor, id).content().mediaIds();
                    default -> throw new PlatformException(400, "资源类型无效");
                };
        if (!allowed.containsAll(ids)) {
            throw new PlatformException(403, "文件不属于该内容");
        }
        return client.post("media", "/internal/media/access", Map.of("mediaIds", ids), Map.class);
    }

    private Set<String> accessibleGroups(List<String> ids) {
        Set<String> allowed = new HashSet<>();
        for (int start = 0; start < ids.size(); start += 200) {
            List<?> results =
                    client.post(
                            "academic",
                            "/internal/academic/groups/batch-access",
                            Map.of("ids", ids.subList(start, Math.min(start + 200, ids.size()))),
                            List.class);
            if (results == null) {
                throw new PlatformException(503, "班级权限服务暂不可用");
            }
            for (Object value : results) {
                if (value instanceof Map<?, ?> row && Boolean.TRUE.equals(row.get("allowed"))) {
                    allowed.add(String.valueOf(row.get("id")));
                }
            }
        }
        return allowed;
    }

    private void validateTemplate(TemplateInput input) {
        if (input.ageMax() < input.ageMin()
                || input.expectedLessons() < 1
                || input.steps() == null
                || input.steps().isEmpty()) {
            throw new PlatformException(400, "年龄或课次配置无效");
        }
    }

    private void validateLessons(Actor actor, ThemeInput input) {
        actor.requireBranch(input.branchId());
        if (new HashSet<>(input.lessonIds()).size() != input.lessonIds().size()) {
            throw new PlatformException(400, "课次不可重复");
        }
        Map<?, ?> result =
                client.post(
                        "academic",
                        "/internal/academic/lessons/validate",
                        Map.of(
                                "groupId",
                                input.groupId(),
                                "branchId",
                                input.branchId(),
                                "lessonIds",
                                input.lessonIds()),
                        Map.class);
        if (result == null || !Boolean.TRUE.equals(result.get("allowed"))) {
            throw new PlatformException(403, "班级或课次无权限");
        }
    }

    private void media(Actor actor, List<String> ids, Set<String> authorizedSourceMedia) {
        if (new HashSet<>(ids).size() > 100) {
            throw new PlatformException(400, "单次内容最多关联 100 个不同媒体文件（含音频）");
        }
        for (String id : new LinkedHashSet<>(ids)) {
            Map<?, ?> result = client.get("media", "/internal/media/" + id + "/usable", Map.class);
            if (result == null
                    || !Boolean.TRUE.equals(result.get("usable"))
                    || !actor.tenantId().equals(String.valueOf(result.get("tenantId")))) {
                throw new PlatformException(400, "媒体尚未校验或不可用");
            }
            if (!actor.userId().equals(String.valueOf(result.get("ownerId")))
                    && !authorizedSourceMedia.contains(id)) {
                throw new PlatformException(403, "新引用媒体必须由本人上传，或从已授权内容显式复制");
            }
        }
    }

    private void references(
            Actor actor, String type, String id, List<String> mediaIds, int revision) {
        outbox.enqueue(
                "media",
                "teaching.media-references",
                actor.tenantId(),
                null,
                id,
                Map.of(
                        "ownerType",
                        type,
                        "ownerId",
                        id,
                        "mediaIds",
                        List.copyOf(new LinkedHashSet<>(mediaIds)),
                        "revision",
                        revision));
    }

    private void audit(Actor actor, String id, String action, String reason, Object content) {
        jdbc.update(
                "INSERT INTO teaching_theme_audit(id,tenant_id,theme_id,actor_id,action_name,reason,content_json,created_at) VALUES (?,?,?,?,?,?,?,?)",
                id(),
                actor.tenantId(),
                id,
                actor.userId(),
                action,
                reason,
                encode(content),
                Timestamp.from(Instant.now()));
    }

    private Template templateRow(ResultSet rs, int n) throws SQLException {
        return new Template(
                rs.getString("id"),
                rs.getString("owner_id"),
                rs.getInt("row_version"),
                decode(rs.getString("content_json"), TemplateInput.class),
                rs.getTimestamp("created_at").toInstant());
    }

    private TemplateVersion versionRow(ResultSet rs, int n) throws SQLException {
        return new TemplateVersion(
                rs.getString("id"),
                rs.getString("template_id"),
                rs.getInt("version_number"),
                decode(rs.getString("content_json"), TemplateInput.class),
                rs.getTimestamp("created_at").toInstant());
    }

    private Resource resourceRow(ResultSet rs, int n) throws SQLException {
        return new Resource(
                rs.getString("id"),
                rs.getString("owner_id"),
                rs.getInt("row_version"),
                rs.getBoolean("published"),
                decode(rs.getString("content_json"), ResourceInput.class),
                rs.getTimestamp("created_at").toInstant());
    }

    private Theme themeRow(ResultSet rs, int n) throws SQLException {
        return new Theme(
                rs.getString("id"),
                rs.getInt("row_version"),
                rs.getString("theme_status"),
                decode(rs.getString("content_json"), ThemeInput.class),
                decode(rs.getString("template_json"), TemplateInput.class),
                rs.getTimestamp("created_at").toInstant());
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("教学内容序列化失败", e);
        }
    }

    private <T> T decode(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("教学内容读取失败", e);
        }
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private static String search(String q) {
        return "%" + (q == null ? "" : q.replace("%", "").replace("_", "")) + "%";
    }

    private static int limit(int limit) {
        return Math.max(1, Math.min(100, limit));
    }

    private static <T> T one(List<T> values) {
        if (values.isEmpty()) {
            throw new PlatformException(404, "内容不存在");
        }
        return values.get(0);
    }

    private static void changed(int rows) {
        if (rows != 1) {
            throw new PlatformException(409, "版本已变化，请刷新后重试");
        }
    }

    private static void checkVersion(int actual, int expected) {
        if (actual != expected) {
            throw new PlatformException(409, "版本已变化，请刷新后重试");
        }
    }
}
