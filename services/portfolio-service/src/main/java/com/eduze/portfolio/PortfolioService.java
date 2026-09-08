package com.eduze.portfolio;

import static com.eduze.portfolio.PortfolioModels.*;

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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Student work, household publications and public exhibitions have separate lifecycles. */
@Service
public class PortfolioService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final InternalClient client;
    private final Outbox outbox;
    private final TransactionTemplate transaction;

    public PortfolioService(
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
        if (!write || !actor.permissions().contains("portfolio:write")) {
            actor.requirePermission(write ? "student:write" : "student:read");
        }
    }

    private String studentAccess(Actor actor, String studentId) {
        Map<?, ?> access =
                client.get(
                        "academic",
                        "/internal/academic/students/" + studentId + "/access",
                        Map.class);
        if (access == null
                || !Boolean.TRUE.equals(access.get("allowed"))
                || access.get("branchId") == null) {
            throw new PlatformException(403, "无学员权限或家庭授权已解除");
        }
        String branch = String.valueOf(access.get("branchId"));
        if (actor.isStaff()) {
            actor.requireBranch(branch);
        }
        return branch;
    }

    public StudentRecord create(Actor actor, DraftInput input) {
        return create(actor, input, null);
    }

    public StudentRecord create(Actor actor, DraftInput input, String sourceRecordId) {
        staff(actor, true);
        String branch = studentAccess(actor, input.studentId());
        validateDraft(actor, input, branch, explicitSourceMedia(actor, sourceRecordId));
        Map<?, ?> theme =
                client.get("teaching", "/internal/teaching/themes/" + input.themeId(), Map.class);
        if (theme == null
                || !(theme.get("content") instanceof Map<?, ?> content)
                || !branch.equals(String.valueOf(content.get("branchId")))) {
            throw new PlatformException(400, "主题与学员校区不一致");
        }
        List<?> roster =
                client.get(
                        "academic",
                        "/internal/academic/groups/" + content.get("groupId") + "/students",
                        List.class);
        if (roster == null
                || roster.stream()
                        .noneMatch(
                                row ->
                                        row instanceof Map<?, ?> student
                                                && input.studentId()
                                                        .equals(
                                                                String.valueOf(
                                                                        student.get("id"))))) {
            throw new PlatformException(400, "学员不属于此主题班级");
        }
        String id = id();
        return transaction.execute(
                tx -> {
                    try {
                        jdbc.update(
                                "INSERT INTO portfolio_record(id,tenant_id,branch_id,theme_id,student_id,row_version,progress_status,publication_status,content_json,created_at) VALUES (?,?,?,?,?,1,?,'DRAFT',?,?)",
                                id,
                                actor.tenantId(),
                                branch,
                                input.themeId(),
                                input.studentId(),
                                input.progress(),
                                encode(input),
                                Timestamp.from(Instant.now()));
                    } catch (DuplicateKeyException e) {
                        throw new PlatformException(409, "该学员已有此主题记录，请继续编辑");
                    }
                    references(actor, branch, "record", id, mediaIds(input), 1);
                    return raw(actor.tenantId(), id);
                });
    }

    public StudentRecord update(Actor actor, String id, DraftInput input) {
        return update(actor, id, input, null);
    }

    public StudentRecord update(Actor actor, String id, DraftInput input, String sourceRecordId) {
        staff(actor, true);
        StudentRecord previous = record(actor, id);
        if (!previous.content().studentId().equals(input.studentId())
                || !previous.content().themeId().equals(input.themeId())) {
            throw new PlatformException(400, "学员和主题不可替换");
        }
        Set<String> retained = authorizedRecordMedia(actor, previous);
        retained.addAll(explicitSourceMedia(actor, sourceRecordId));
        validateDraft(actor, input, previous.branchId(), retained);
        return transaction.execute(
                tx -> {
                    changed(
                            jdbc.update(
                                    "UPDATE portfolio_record SET content_json=?,progress_status=?,needs_publishing=TRUE,row_version=row_version+1 WHERE id=? AND tenant_id=? AND row_version=?",
                                    encode(input),
                                    input.progress(),
                                    id,
                                    actor.tenantId(),
                                    input.version()));
                    references(
                            actor,
                            previous.branchId(),
                            "record",
                            id,
                            mediaIds(input),
                            input.version() + 1);
                    audit(actor, id, "UPDATE", "更新课效草稿", input);
                    return raw(actor.tenantId(), id);
                });
    }

    public StudentRecord record(Actor actor, String id) {
        StudentRecord record = raw(actor.tenantId(), id);
        studentAccess(actor, record.content().studentId());
        if (actor.isStaff()) {
            staff(actor, false);
            return record;
        }
        Publication publication = activePublication(actor.tenantId(), id);
        return new StudentRecord(
                record.id(),
                publication.version(),
                record.branchId(),
                "PUBLISHED",
                householdContent(publication.content()),
                publication.createdAt());
    }

    public List<StudentRecord> records(Actor actor, String themeId, String studentId, int limit) {
        if (!actor.isStaff() && (studentId == null || studentId.isBlank())) {
            throw new PlatformException(400, "请选择孩子");
        }
        if (actor.isStaff()) {
            staff(actor, false);
        }
        if (studentId != null && !studentId.isBlank()) {
            studentAccess(actor, studentId);
        }
        StringBuilder sql =
                new StringBuilder(
                        "SELECT id,row_version,branch_id,publication_status,content_json,created_at,needs_publishing FROM portfolio_record WHERE tenant_id=?");
        List<Object> args = new ArrayList<>();
        args.add(actor.tenantId());
        if (themeId != null && !themeId.isBlank()) {
            sql.append(" AND theme_id=?");
            args.add(themeId);
        }
        if (studentId != null && !studentId.isBlank()) {
            sql.append(" AND student_id=?");
            args.add(studentId);
        }
        if (!actor.isStaff()) {
            sql.append(" AND publication_status='PUBLISHED'");
        }
        List<StudentRecord> result = new ArrayList<>();
        String cursor = null;
        Instant before = null;
        while (result.size() < limit(limit)) {
            List<Object> pageArgs = new ArrayList<>(args);
            String seek = "";
            if (before != null) {
                seek = " AND (created_at<? OR (created_at=? AND id<?))";
                pageArgs.add(Timestamp.from(before));
                pageArgs.add(Timestamp.from(before));
                pageArgs.add(cursor);
            }
            pageArgs.add(200);
            List<StudentRecord> page =
                    jdbc.query(
                            sql + seek + " ORDER BY created_at DESC,id DESC LIMIT ?",
                            this::recordRow,
                            pageArgs.toArray());
            if (page.isEmpty()) {
                break;
            }
            cursor = page.get(page.size() - 1).id();
            before = page.get(page.size() - 1).createdAt();
            List<StudentRecord> scoped =
                    page.stream()
                            .filter(
                                    r ->
                                            !actor.isStaff()
                                                    || actor.isSuperAdmin()
                                                    || actor.branchIds().contains(r.branchId()))
                            .toList();
            Set<String> allowed =
                    actor.isStaff()
                            ? accessibleStudents(
                                    scoped.stream()
                                            .map(r -> r.content().studentId())
                                            .distinct()
                                            .toList())
                            : Set.of(studentId);
            for (StudentRecord row : scoped) {
                if (!allowed.contains(row.content().studentId())) {
                    continue;
                }
                if (actor.isStaff()) {
                    result.add(row);
                } else {
                    Publication publication = activePublication(actor.tenantId(), row.id());
                    result.add(
                            new StudentRecord(
                                    row.id(),
                                    publication.version(),
                                    row.branchId(),
                                    "PUBLISHED",
                                    householdContent(publication.content()),
                                    publication.createdAt()));
                }
                if (result.size() == limit(limit)) {
                    break;
                }
            }
        }
        return List.copyOf(result);
    }

    public Publication publish(Actor actor, String id, int version, String key) {
        staff(actor, true);
        StudentRecord preflight = record(actor, id);
        validateDraft(
                actor,
                preflight.content(),
                preflight.branchId(),
                authorizedRecordMedia(actor, preflight));
        return transaction.execute(
                tx -> {
                    lock(actor.tenantId(), id);
                    List<Publication> repeated =
                            jdbc.query(
                                    "SELECT id,record_id,version_number,content_json,created_at FROM portfolio_publication WHERE tenant_id=? AND record_id=? AND idempotency_key=? FOR UPDATE",
                                    this::publicationRow,
                                    actor.tenantId(),
                                    id,
                                    key);
                    if (!repeated.isEmpty()) {
                        return repeated.get(0);
                    }
                    StudentRecord previous =
                            one(
                                    jdbc.query(
                                            "SELECT id,row_version,branch_id,publication_status,content_json,created_at,needs_publishing FROM portfolio_record WHERE tenant_id=? AND id=? FOR UPDATE",
                                            this::recordRow,
                                            actor.tenantId(),
                                            id));
                    checkVersion(previous.version(), version);
                    Publication publication =
                            new Publication(
                                    id(), id, version + 1, previous.content(), Instant.now());
                    jdbc.update(
                            "INSERT INTO portfolio_publication(id,tenant_id,record_id,student_id,version_number,idempotency_key,content_json,created_at) VALUES (?,?,?,?,?,?,?,?)",
                            publication.id(),
                            actor.tenantId(),
                            id,
                            previous.content().studentId(),
                            publication.version(),
                            key,
                            encode(publication.content()),
                            Timestamp.from(publication.createdAt()));
                    changed(
                            jdbc.update(
                                    "UPDATE portfolio_record SET publication_status='PUBLISHED',needs_publishing=FALSE,current_publication_id=?,row_version=row_version+1 WHERE tenant_id=? AND id=? AND row_version=?",
                                    publication.id(),
                                    actor.tenantId(),
                                    id,
                                    version));
                    references(
                            actor,
                            previous.branchId(),
                            "publication",
                            publication.id(),
                            mediaIds(publication.content()),
                            1);
                    outbox.enqueue(
                            "notification",
                            "portfolio.published",
                            actor.tenantId(),
                            previous.branchId(),
                            id,
                            event(previous, publication.id(), publication.version()));
                    audit(actor, id, "PUBLISH", "发布家庭课效", publication);
                    return publication;
                });
    }

    public StudentRecord withdraw(Actor actor, String id, int version, String reason) {
        staff(actor, true);
        StudentRecord previous = record(actor, id);
        if (reason == null || reason.isBlank()) {
            throw new PlatformException(400, "撤回需填写原因");
        }
        return transaction.execute(
                tx -> {
                    changed(
                            jdbc.update(
                                    "UPDATE portfolio_record SET publication_status='WITHDRAWN',needs_publishing=TRUE,row_version=row_version+1 WHERE tenant_id=? AND id=? AND row_version=?",
                                    actor.tenantId(),
                                    id,
                                    version));
                    jdbc.update(
                            "UPDATE portfolio_publication SET revoked=TRUE WHERE tenant_id=? AND record_id=?",
                            actor.tenantId(),
                            id);
                    outbox.enqueue(
                            "notification",
                            "portfolio.withdrawn",
                            actor.tenantId(),
                            previous.branchId(),
                            id,
                            event(previous, "", version + 1));
                    audit(actor, id, "WITHDRAW", reason, previous);
                    return raw(actor.tenantId(), id);
                });
    }

    public List<Map<String, Object>> roster(Actor actor, String themeId) {
        staff(actor, false);
        Map<?, ?> theme = client.get("teaching", "/internal/teaching/themes/" + themeId, Map.class);
        if (theme == null || !(theme.get("content") instanceof Map<?, ?> content)) {
            throw new PlatformException(404, "主题不存在");
        }
        actor.requireBranch(String.valueOf(content.get("branchId")));
        List<?> students =
                client.get(
                        "academic",
                        "/internal/academic/groups/" + content.get("groupId") + "/students",
                        List.class);
        if (students == null) {
            throw new PlatformException(503, "学员名单暂不可用");
        }
        Map<String, StudentRecord> records = new HashMap<>();
        List<StudentRecord> themeRecords =
                jdbc.query(
                        "SELECT id,row_version,branch_id,publication_status,content_json,created_at,needs_publishing FROM portfolio_record WHERE tenant_id=? AND theme_id=? ORDER BY id",
                        this::recordRow,
                        actor.tenantId(),
                        themeId);
        Set<String> allowed =
                accessibleStudents(
                        themeRecords.stream()
                                .map(r -> r.content().studentId())
                                .distinct()
                                .toList());
        for (StudentRecord r : themeRecords) {
            if (allowed.contains(r.content().studentId())) {
                records.put(r.content().studentId(), r);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object value : students) {
            if (!(value instanceof Map<?, ?> student)) {
                continue;
            }
            String studentId = String.valueOf(student.get("id"));
            StudentRecord record = records.remove(studentId);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", studentId);
            row.put("name", student.get("name"));
            row.put("recordId", record == null ? null : record.id());
            row.put("needsPublishing", record == null || record.needsPublishing());
            row.put("progress", record == null ? "NOT_STARTED" : record.content().progress());
            row.put("status", record == null ? "DRAFT" : record.status());
            result.add(row);
        }
        for (StudentRecord record : records.values()) {
            result.add(
                    Map.of(
                            "studentId",
                            record.content().studentId(),
                            "name",
                            "历史学员",
                            "recordId",
                            record.id(),
                            "progress",
                            record.content().progress(),
                            "status",
                            record.status(),
                            "needsPublishing",
                            record.needsPublishing()));
        }
        return result;
    }

    public Map<String, Long> dashboard(Actor actor, String branch) {
        staff(actor, false);
        if (branch != null && !branch.isBlank()) {
            actor.requireBranch(branch);
        }
        List<Map<String, Object>> rows =
                jdbc
                        .query(
                                "SELECT branch_id,student_id,publication_status,progress_status,needs_publishing,COUNT(*) FROM portfolio_record WHERE tenant_id=? GROUP BY branch_id,student_id,publication_status,progress_status,needs_publishing",
                                (rs, n) ->
                                        Map.<String, Object>of(
                                                "branch",
                                                rs.getString(1),
                                                "student",
                                                rs.getString(2),
                                                "status",
                                                rs.getString(3),
                                                "progress",
                                                rs.getString(4),
                                                "needsPublishing",
                                                rs.getBoolean(5),
                                                "total",
                                                rs.getLong(6)),
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
                accessibleStudents(
                        rows.stream().map(r -> (String) r.get("student")).distinct().toList());
        Map<String, Long> counts =
                new HashMap<>(
                        Map.of(
                                "draft",
                                0L,
                                "published",
                                0L,
                                "withdrawn",
                                0L,
                                "makeupPending",
                                0L,
                                "needsPublishing",
                                0L));
        Map<String, String> keys =
                Map.of("DRAFT", "draft", "PUBLISHED", "published", "WITHDRAWN", "withdrawn");
        for (Map<String, Object> row : rows) {
            if (!allowed.contains(row.get("student"))) {
                continue;
            }
            counts.merge(keys.get(row.get("status")), (Long) row.get("total"), Long::sum);
            if (Boolean.TRUE.equals(row.get("needsPublishing"))) {
                counts.merge("needsPublishing", (Long) row.get("total"), Long::sum);
            }
            if ("MAKEUP_PENDING".equals(row.get("progress"))) {
                counts.merge("makeupPending", (Long) row.get("total"), Long::sum);
            }
        }
        return counts;
    }

    public Object mediaAccess(Actor actor, String id, List<String> ids) {
        StudentRecord record = record(actor, id);
        if (!mediaIds(record.content()).containsAll(ids)) {
            throw new PlatformException(403, "文件不属于该课效");
        }
        return client.post("media", "/internal/media/access", Map.of("mediaIds", ids), Map.class);
    }

    public Map<String, Object> growth(Actor actor, String studentId) {
        studentAccess(actor, studentId);
        List<Publication> publications =
                jdbc.query(
                        "SELECT p.id,p.record_id,p.version_number,p.content_json,p.created_at FROM portfolio_publication p JOIN portfolio_record r ON r.current_publication_id=p.id AND r.tenant_id=p.tenant_id WHERE p.tenant_id=? AND p.student_id=? AND r.publication_status='PUBLISHED' ORDER BY p.created_at DESC LIMIT 100",
                        this::publicationRow,
                        actor.tenantId(),
                        studentId);
        if (!actor.isStaff()) {
            publications =
                    publications.stream()
                            .map(
                                    p ->
                                            new Publication(
                                                    p.id(),
                                                    p.recordId(),
                                                    p.version(),
                                                    householdContent(p.content()),
                                                    p.createdAt()))
                            .toList();
        }
        List<Report> reports =
                jdbc.query(
                        "SELECT content_json FROM portfolio_report WHERE tenant_id=? AND student_id=? ORDER BY created_at DESC LIMIT 100",
                        (rs, n) -> decode(rs.getString(1), Report.class),
                        actor.tenantId(),
                        studentId);
        reports =
                reports.stream()
                        .map(
                                r ->
                                        new Report(
                                                r.id(),
                                                r.studentId(),
                                                r.title(),
                                                r.summary(),
                                                r.publications().stream()
                                                        .filter(
                                                                p ->
                                                                        isPublicationVisible(
                                                                                actor.tenantId(),
                                                                                p.id()))
                                                        .map(
                                                                p ->
                                                                        actor.isStaff()
                                                                                ? p
                                                                                : new Publication(
                                                                                        p.id(),
                                                                                        p
                                                                                                .recordId(),
                                                                                        p.version(),
                                                                                        householdContent(
                                                                                                p
                                                                                                        .content()),
                                                                                        p
                                                                                                .createdAt()))
                                                        .toList(),
                                                r.createdAt()))
                        .toList();
        return Map.of(
                "studentId",
                studentId,
                "publications",
                publications,
                "assessments",
                assessments(actor, studentId),
                "reports",
                reports);
    }

    @Transactional
    public Report report(Actor actor, String studentId, ReportInput input) {
        staff(actor, true);
        studentAccess(actor, studentId);
        List<Publication> publications = new ArrayList<>();
        for (String publicationId : new LinkedHashSet<>(input.publicationIds())) {
            Publication publication = publication(actor.tenantId(), publicationId);
            if (!publication.content().studentId().equals(studentId)
                    || !isPublicationVisible(actor.tenantId(), publication.id())) {
                throw new PlatformException(400, "只能选取该学员当前可见的已发布作品");
            }
            publications.add(publication);
        }
        enforceMediaLimit(
                publications.stream()
                        .flatMap(publication -> mediaIds(publication.content()).stream())
                        .toList());
        Report report =
                new Report(
                        id(),
                        studentId,
                        input.title(),
                        input.summary(),
                        publications,
                        Instant.now());
        jdbc.update(
                "INSERT INTO portfolio_report(id,tenant_id,student_id,content_json,created_at) VALUES (?,?,?,?,?)",
                report.id(),
                actor.tenantId(),
                studentId,
                encode(report),
                Timestamp.from(report.createdAt()));
        return report;
    }

    public List<Assessment> assessments(Actor actor, String studentId) {
        studentAccess(actor, studentId);
        if (actor.isStaff()) {
            staff(actor, false);
        }
        return jdbc.query(
                "SELECT content_json FROM portfolio_assessment WHERE tenant_id=? AND student_id=? ORDER BY created_at DESC LIMIT 100",
                (rs, n) -> decode(rs.getString(1), Assessment.class),
                actor.tenantId(),
                studentId);
    }

    @Transactional
    public Assessment assess(Actor actor, String studentId, AssessmentInput input) {
        staff(actor, true);
        String branch = studentAccess(actor, studentId);
        Map<?, ?> stage =
                client.get("teaching", "/internal/teaching/stages/" + input.stageId(), Map.class);
        if (stage == null) {
            throw new PlatformException(400, "教学阶段不存在");
        }
        if (input.scores() == null
                || input.scores().values().stream().anyMatch(v -> v == null || v < 1 || v > 5)) {
            throw new PlatformException(400, "评价分值必须为 1–5");
        }
        Assessment assessment =
                new Assessment(
                        id(),
                        studentId,
                        input.stageId(),
                        String.valueOf(stage.get("code")),
                        String.valueOf(stage.get("name")),
                        input.assessedAt(),
                        actor.userId(),
                        input.scores(),
                        input.comment(),
                        Instant.now());
        jdbc.update(
                "INSERT INTO portfolio_assessment(id,tenant_id,student_id,branch_id,content_json,created_at) VALUES (?,?,?,?,?,?)",
                assessment.id(),
                actor.tenantId(),
                studentId,
                branch,
                encode(assessment),
                Timestamp.from(assessment.createdAt()));
        return assessment;
    }

    /** Fixed historical publications remain readable until explicitly withdrawn. */
    public Object publicationMedia(Actor actor, String publicationId, List<String> ids) {
        Publication publication = publication(actor.tenantId(), publicationId);
        studentAccess(actor, publication.content().studentId());
        if (actor.isStaff()) {
            staff(actor, false);
        }
        if (!isPublicationVisible(actor.tenantId(), publicationId)) {
            throw new PlatformException(404, "发布版本已撤回");
        }
        if (!mediaIds(publication.content()).containsAll(ids)) {
            throw new PlatformException(403, "文件不属于该发布版本");
        }
        return client.post("media", "/internal/media/access", Map.of("mediaIds", ids), Map.class);
    }

    public Object reportMedia(
            Actor actor, String reportId, String publicationId, List<String> ids) {
        Report report =
                one(
                        jdbc.query(
                                "SELECT content_json FROM portfolio_report WHERE tenant_id=? AND id=?",
                                (rs, n) -> decode(rs.getString(1), Report.class),
                                actor.tenantId(),
                                reportId));
        studentAccess(actor, report.studentId());
        if (report.publications().stream().noneMatch(p -> p.id().equals(publicationId))) {
            throw new PlatformException(403, "发布版本不属于此报告");
        }
        return publicationMedia(actor, publicationId, ids);
    }

    public List<Report> collections(Actor actor, String studentId) {
        studentAccess(actor, studentId);
        if (actor.isStaff()) {
            staff(actor, false);
        }
        return jdbc
                .query(
                        "SELECT content_json FROM portfolio_collection WHERE tenant_id=? AND student_id=? ORDER BY created_at DESC",
                        (rs, n) -> decode(rs.getString(1), Report.class),
                        actor.tenantId(),
                        studentId)
                .stream()
                .map(
                        report ->
                                new Report(
                                        report.id(),
                                        report.studentId(),
                                        report.title(),
                                        report.summary(),
                                        report.publications().stream()
                                                .filter(
                                                        p ->
                                                                isPublicationVisible(
                                                                        actor.tenantId(), p.id()))
                                                .map(
                                                        p ->
                                                                actor.isStaff()
                                                                        ? p
                                                                        : new Publication(
                                                                                p.id(),
                                                                                p.recordId(),
                                                                                p.version(),
                                                                                householdContent(
                                                                                        p
                                                                                                .content()),
                                                                                p.createdAt()))
                                                .toList(),
                                        report.createdAt()))
                .toList();
    }

    public Report createCollection(Actor actor, String studentId, ReportInput input) {
        studentAccess(actor, studentId);
        if (actor.isStaff()) {
            staff(actor, true);
        }
        List<Publication> selected = new ArrayList<>();
        for (String id : new LinkedHashSet<>(input.publicationIds())) {
            Publication publication = publication(actor.tenantId(), id);
            if (!studentId.equals(publication.content().studentId())
                    || !isPublicationVisible(actor.tenantId(), id)) {
                throw new PlatformException(400, "只能精选该孩子有效发布版本");
            }
            selected.add(
                    actor.isStaff()
                            ? publication
                            : new Publication(
                                    publication.id(),
                                    publication.recordId(),
                                    publication.version(),
                                    householdContent(publication.content()),
                                    publication.createdAt()));
        }
        enforceMediaLimit(
                selected.stream()
                        .flatMap(publication -> mediaIds(publication.content()).stream())
                        .toList());
        Report collection =
                new Report(
                        id(), studentId, input.title(), input.summary(), selected, Instant.now());
        jdbc.update(
                "INSERT INTO portfolio_collection(id,tenant_id,student_id,content_json,created_at) VALUES (?,?,?,?,?)",
                collection.id(),
                actor.tenantId(),
                studentId,
                encode(collection),
                Timestamp.from(collection.createdAt()));
        return collection;
    }

    public Entry addEntry(Actor actor, String recordId, EntryInput input) {
        staff(actor, true);
        StudentRecord record = authorizedRaw(actor, recordId);
        Map<?, ?> theme =
                client.get(
                        "teaching",
                        "/internal/teaching/themes/" + record.content().themeId(),
                        Map.class);
        if (theme == null
                || !(theme.get("content") instanceof Map<?, ?> content)
                || !(content.get("lessonIds") instanceof List<?> lessons)
                || !lessons.contains(input.lessonId())) {
            throw new PlatformException(400, "课堂记录必须关联当前主题的有效课次；补课请先关联至主题");
        }
        usable(actor, input.mediaIds(), authorizedRecordMedia(actor, record));
        return transaction.execute(
                tx -> {
                    lock(actor.tenantId(), recordId);
                    List<Entry> previous =
                            jdbc.query(
                                    "SELECT id,record_id,entry_status,content_json,created_at FROM portfolio_entry WHERE tenant_id=? AND record_id=? AND command_key=? FOR UPDATE",
                                    this::entryRow,
                                    actor.tenantId(),
                                    recordId,
                                    input.idempotencyKey());
                    if (!previous.isEmpty()) {
                        Entry existing = previous.get(0);
                        if (!existing.lessonId().equals(input.lessonId())
                                || !existing.notes().equals(input.notes())
                                || !existing.mediaIds().equals(input.mediaIds())
                                || !existing.occurredAt().equals(input.occurredAt())) {
                            throw new PlatformException(409, "幂等键已被另一条课堂记录使用");
                        }
                        return existing;
                    }
                    String id = id();
                    Instant now = Instant.now();
                    jdbc.update(
                            "INSERT INTO portfolio_entry(id,tenant_id,record_id,student_id,lesson_id,entry_status,row_version,command_key,content_json,created_at) VALUES (?,?,?,?,?,'DRAFT',1,?,?,?)",
                            id,
                            actor.tenantId(),
                            recordId,
                            record.content().studentId(),
                            input.lessonId(),
                            input.idempotencyKey(),
                            encode(input),
                            Timestamp.from(now));
                    references(actor, record.branchId(), "entry", id, input.mediaIds(), 1);
                    audit(
                            actor,
                            recordId,
                            "ENTRY_ADD",
                            "追加课堂过程",
                            Map.of("entryId", id, "content", input));
                    return new Entry(
                            id,
                            recordId,
                            input.lessonId(),
                            input.occurredAt(),
                            input.notes(),
                            input.mediaIds(),
                            "DRAFT",
                            now);
                });
    }

    public List<Entry> entries(Actor actor, String recordId) {
        authorizedRaw(actor, recordId);
        String visibility = actor.isStaff() ? "" : " AND entry_status='PUBLISHED'";
        return jdbc.query(
                "SELECT id,record_id,entry_status,content_json,created_at FROM portfolio_entry WHERE tenant_id=? AND record_id=?"
                        + visibility
                        + " ORDER BY created_at,id",
                this::entryRow,
                actor.tenantId(),
                recordId);
    }

    public List<Entry> studentEntries(Actor actor, String studentId) {
        studentAccess(actor, studentId);
        if (actor.isStaff()) {
            staff(actor, false);
        }
        return jdbc.query(
                "SELECT id,record_id,entry_status,content_json,created_at FROM portfolio_entry WHERE tenant_id=? AND student_id=?"
                        + (actor.isStaff() ? "" : " AND entry_status='PUBLISHED'")
                        + " ORDER BY created_at DESC,id",
                this::entryRow,
                actor.tenantId(),
                studentId);
    }

    public Entry publishEntry(Actor actor, String recordId, String entryId, String key) {
        staff(actor, true);
        StudentRecord record = authorizedRaw(actor, recordId);
        Entry before = entry(actor.tenantId(), recordId, entryId, false);
        usable(actor, before.mediaIds(), new HashSet<>(before.mediaIds()));
        return transaction.execute(
                tx -> {
                    Entry current = entry(actor.tenantId(), recordId, entryId, true);
                    if ("PUBLISHED".equals(current.status())) {
                        return current;
                    }
                    if ("WITHDRAWN".equals(current.status())) {
                        throw new PlatformException(409, "撤回的课堂动态不可重用，请新增更正记录");
                    }
                    jdbc.update(
                            "UPDATE portfolio_entry SET entry_status='PUBLISHED',row_version=row_version+1 WHERE tenant_id=? AND id=? AND entry_status='DRAFT'",
                            actor.tenantId(),
                            entryId);
                    outbox.enqueue(
                            "notification",
                            "portfolio.classroom-published",
                            actor.tenantId(),
                            record.branchId(),
                            entryId,
                            entryEvent(record, entryId, 2));
                    audit(
                            actor,
                            recordId,
                            "ENTRY_PUBLISH",
                            "发布课堂动态",
                            Map.of("entryId", entryId, "idempotencyKey", key));
                    return entry(actor.tenantId(), recordId, entryId, false);
                });
    }

    public Entry withdrawEntry(Actor actor, String recordId, String entryId, String reason) {
        staff(actor, true);
        StudentRecord record = authorizedRaw(actor, recordId);
        return transaction.execute(
                tx -> {
                    Entry current = entry(actor.tenantId(), recordId, entryId, true);
                    if ("WITHDRAWN".equals(current.status())) {
                        return current;
                    }
                    jdbc.update(
                            "UPDATE portfolio_entry SET entry_status='WITHDRAWN',row_version=row_version+1 WHERE tenant_id=? AND id=?",
                            actor.tenantId(),
                            entryId);
                    int revision =
                            jdbc.queryForObject(
                                    "SELECT row_version FROM portfolio_entry WHERE tenant_id=? AND id=?",
                                    Integer.class,
                                    actor.tenantId(),
                                    entryId);
                    outbox.enqueue(
                            "notification",
                            "portfolio.classroom-withdrawn",
                            actor.tenantId(),
                            record.branchId(),
                            entryId,
                            entryEvent(record, entryId, revision));
                    audit(actor, recordId, "ENTRY_WITHDRAW", reason, Map.of("entryId", entryId));
                    return entry(actor.tenantId(), recordId, entryId, false);
                });
    }

    public Object entryMedia(Actor actor, String recordId, String entryId, List<String> ids) {
        authorizedRaw(actor, recordId);
        Entry entry = entry(actor.tenantId(), recordId, entryId, false);
        if (!actor.isStaff() && !"PUBLISHED".equals(entry.status())) {
            throw new PlatformException(404, "课堂动态未发布或已撤回");
        }
        if (!entry.mediaIds().containsAll(ids)) {
            throw new PlatformException(403, "文件不属于此课堂动态");
        }
        return client.post("media", "/internal/media/access", Map.of("mediaIds", ids), Map.class);
    }

    public List<Map<String, Object>> history(Actor actor, String recordId) {
        staff(actor, false);
        authorizedRaw(actor, recordId);
        return jdbc.query(
                "SELECT id,actor_id,action_name,reason,content_json,created_at FROM portfolio_audit WHERE tenant_id=? AND record_id=? ORDER BY created_at,id",
                (rs, n) ->
                        Map.<String, Object>of(
                                "id",
                                rs.getString(1),
                                "actorId",
                                rs.getString(2),
                                "action",
                                rs.getString(3),
                                "reason",
                                rs.getString(4),
                                "content",
                                decode(rs.getString(5), Object.class),
                                "createdAt",
                                rs.getTimestamp(6).toInstant()),
                actor.tenantId(),
                recordId);
    }

    private StudentRecord authorizedRaw(Actor actor, String id) {
        StudentRecord record = raw(actor.tenantId(), id);
        studentAccess(actor, record.content().studentId());
        if (actor.isStaff()) {
            staff(actor, false);
        }
        return record;
    }

    private Entry entry(String tenant, String recordId, String id, boolean locking) {
        return one(
                jdbc.query(
                        "SELECT id,record_id,entry_status,content_json,created_at FROM portfolio_entry WHERE tenant_id=? AND record_id=? AND id=?"
                                + (locking ? " FOR UPDATE" : ""),
                        this::entryRow,
                        tenant,
                        recordId,
                        id));
    }

    private Entry entryRow(ResultSet rs, int index) throws SQLException {
        EntryInput content = decode(rs.getString("content_json"), EntryInput.class);
        return new Entry(
                rs.getString("id"),
                rs.getString("record_id"),
                content.lessonId(),
                content.occurredAt(),
                content.notes(),
                content.mediaIds(),
                rs.getString("entry_status"),
                rs.getTimestamp("created_at").toInstant());
    }

    private Map<String, Object> entryEvent(StudentRecord record, String id, int version) {
        return Map.of(
                "recordId",
                record.id(),
                "publicationId",
                id,
                "entryId",
                id,
                "studentId",
                record.content().studentId(),
                "branchId",
                record.branchId(),
                "title",
                "课堂创作动态",
                "version",
                version);
    }

    /**
     * Only invoke after authorizing the source record against the current actor's live student ACL.
     */
    private Set<String> authorizedRecordMedia(Actor actor, StudentRecord source) {
        Set<String> ids = new HashSet<>(mediaIds(source.content()));
        List<Entry> process =
                jdbc.query(
                        "SELECT id,record_id,entry_status,content_json,created_at FROM portfolio_entry WHERE tenant_id=? AND record_id=? AND entry_status<>'WITHDRAWN'",
                        this::entryRow,
                        actor.tenantId(),
                        source.id());
        process.forEach(entry -> ids.addAll(entry.mediaIds()));
        return ids;
    }

    private Set<String> explicitSourceMedia(Actor actor, String sourceRecordId) {
        if (sourceRecordId == null || sourceRecordId.isBlank()) {
            return Set.of();
        }
        StudentRecord source = authorizedRaw(actor, sourceRecordId);
        return authorizedRecordMedia(actor, source);
    }

    private void usable(Actor actor, List<String> ids, Set<String> authorizedSourceMedia) {
        enforceMediaLimit(ids);
        for (String id : new LinkedHashSet<>(ids)) {
            Map<?, ?> media = client.get("media", "/internal/media/" + id + "/usable", Map.class);
            if (media == null
                    || !Boolean.TRUE.equals(media.get("usable"))
                    || !actor.tenantId().equals(String.valueOf(media.get("tenantId")))) {
                throw new PlatformException(400, "文件尚未完成校验或无权使用");
            }
            if (!actor.userId().equals(String.valueOf(media.get("ownerId")))
                    && !authorizedSourceMedia.contains(id)) {
                throw new PlatformException(403, "新引用媒体必须由本人上传，或指定有访问权限的来源记录");
            }
        }
    }

    private Set<String> accessibleStudents(List<String> ids) {
        Set<String> allowed = new HashSet<>();
        for (int start = 0; start < ids.size(); start += 200) {
            List<?> results =
                    client.post(
                            "academic",
                            "/internal/academic/students/batch-access",
                            Map.of("ids", ids.subList(start, Math.min(start + 200, ids.size()))),
                            List.class);
            if (results == null) {
                throw new PlatformException(503, "学员权限服务暂不可用");
            }
            for (Object value : results) {
                if (value instanceof Map<?, ?> row && Boolean.TRUE.equals(row.get("allowed"))) {
                    allowed.add(String.valueOf(row.get("id")));
                }
            }
        }
        return allowed;
    }

    private void validateDraft(
            Actor actor, DraftInput input, String branch, Set<String> authorizedSourceMedia) {
        enforceMediaLimit(mediaIds(input));
        if (!Set.of("NOT_STARTED", "IN_PROGRESS", "MAKEUP_PENDING", "COMPLETED")
                .contains(input.progress())) {
            throw new PlatformException(400, "创作进度无效");
        }
        Set<String> workIds = new HashSet<>();
        for (Artwork work : input.artworks()) {
            if (!workIds.add(work.id())
                    || !Set.of("PROCESS", "FINAL").contains(work.kind())
                    || !work.participantIds().contains(input.studentId())
                    || work.mediaIds().isEmpty()) {
                throw new PlatformException(400, "作品归属、类型或文件无效");
            }
            for (String participant : new HashSet<>(work.participantIds())) {
                if (!branch.equals(studentAccess(actor, participant))) {
                    throw new PlatformException(403, "合作作品参与者校区或权限不一致");
                }
            }
        }
        usable(actor, mediaIds(input), authorizedSourceMedia);
    }

    private DraftInput householdContent(DraftInput input) {
        List<Artwork> works =
                input.artworks().stream()
                        .filter(w -> w.participantIds().contains(input.studentId()))
                        .map(
                                w ->
                                        new Artwork(
                                                w.id(),
                                                w.title(),
                                                w.kind(),
                                                w.mediaIds(),
                                                List.of(input.studentId()),
                                                w.story()))
                        .toList();
        return new DraftInput(
                input.version(),
                input.themeId(),
                input.studentId(),
                input.progress(),
                input.classroomNote(),
                input.comment(),
                works,
                input.audioMediaIds());
    }

    private StudentRecord raw(String tenantId, String id) {
        return one(
                jdbc.query(
                        "SELECT id,row_version,branch_id,publication_status,content_json,created_at,needs_publishing FROM portfolio_record WHERE tenant_id=? AND id=?",
                        this::recordRow,
                        tenantId,
                        id));
    }

    private Publication activePublication(String tenantId, String id) {
        List<Publication> rows =
                jdbc.query(
                        "SELECT p.id,p.record_id,p.version_number,p.content_json,p.created_at FROM portfolio_publication p JOIN portfolio_record r ON r.current_publication_id=p.id AND r.tenant_id=p.tenant_id WHERE r.tenant_id=? AND r.id=? AND r.publication_status='PUBLISHED'",
                        this::publicationRow,
                        tenantId,
                        id);
        if (rows.isEmpty()) {
            throw new PlatformException(404, "课效尚未发布或已经撤回");
        }
        return rows.get(0);
    }

    Publication publication(String tenantId, String id) {
        return one(
                jdbc.query(
                        "SELECT id,record_id,version_number,content_json,created_at FROM portfolio_publication WHERE tenant_id=? AND id=?",
                        this::publicationRow,
                        tenantId,
                        id));
    }

    boolean isPublicationVisible(String tenantId, String publicationId) {
        Integer count =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM portfolio_record r JOIN portfolio_publication p ON p.record_id=r.id AND p.tenant_id=r.tenant_id WHERE p.tenant_id=? AND p.id=? AND p.revoked=FALSE AND r.publication_status='PUBLISHED'",
                        Integer.class,
                        tenantId,
                        publicationId);
        return count != null && count > 0;
    }

    private void lock(String tenantId, String id) {
        jdbc.queryForObject(
                "SELECT row_version FROM portfolio_record WHERE tenant_id=? AND id=? FOR UPDATE",
                Integer.class,
                tenantId,
                id);
    }

    private void references(
            Actor actor,
            String branch,
            String type,
            String id,
            List<String> mediaIds,
            int revision) {
        enforceMediaLimit(mediaIds);
        outbox.enqueue(
                "media",
                "portfolio.media-references",
                actor.tenantId(),
                branch,
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
                "INSERT INTO portfolio_audit(id,tenant_id,record_id,actor_id,action_name,reason,content_json,created_at) VALUES (?,?,?,?,?,?,?,?)",
                id(),
                actor.tenantId(),
                id,
                actor.userId(),
                action,
                reason,
                encode(content),
                Timestamp.from(Instant.now()));
    }

    private Map<String, Object> event(StudentRecord record, String publicationId, int version) {
        return Map.of(
                "recordId",
                record.id(),
                "publicationId",
                publicationId,
                "studentId",
                record.content().studentId(),
                "branchId",
                record.branchId(),
                "title",
                "主题学习回顾",
                "version",
                version);
    }

    private void enforceMediaLimit(Collection<String> mediaIds) {
        if (new HashSet<>(mediaIds).size() > 100) {
            throw new PlatformException(400, "单次内容最多关联 100 个不同媒体文件（含音频）");
        }
    }

    private List<String> mediaIds(DraftInput input) {
        Set<String> ids = new LinkedHashSet<>(input.audioMediaIds());
        for (Artwork artwork : input.artworks()) {
            ids.addAll(artwork.mediaIds());
        }
        return List.copyOf(ids);
    }

    private StudentRecord recordRow(ResultSet rs, int n) throws SQLException {
        return new StudentRecord(
                rs.getString("id"),
                rs.getInt("row_version"),
                rs.getString("branch_id"),
                rs.getString("publication_status"),
                decode(rs.getString("content_json"), DraftInput.class),
                rs.getTimestamp("created_at").toInstant(),
                rs.getBoolean("needs_publishing"));
    }

    private Publication publicationRow(ResultSet rs, int n) throws SQLException {
        return new Publication(
                rs.getString("id"),
                rs.getString("record_id"),
                rs.getInt("version_number"),
                decode(rs.getString("content_json"), DraftInput.class),
                rs.getTimestamp("created_at").toInstant());
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("作品序列化失败", e);
        }
    }

    private <T> T decode(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("作品读取失败", e);
        }
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private static int limit(int limit) {
        return Math.max(1, Math.min(100, limit));
    }

    private static <T> T one(List<T> rows) {
        if (rows.isEmpty()) {
            throw new PlatformException(404, "作品记录不存在");
        }
        return rows.get(0);
    }

    private static void changed(int count) {
        if (count != 1) {
            throw new PlatformException(409, "版本已变化，请刷新后重试");
        }
    }

    private static void checkVersion(int actual, int expected) {
        if (actual != expected) {
            throw new PlatformException(409, "版本已变化，请刷新后重试");
        }
    }
}
