package com.eduze.portfolio;

import static com.eduze.portfolio.PortfolioModels.*;

import com.eduze.platform.runtime.Actor;
import com.eduze.platform.runtime.InternalClient;
import com.eduze.platform.runtime.PlatformException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public selection contains only fixed publications and separately revocable guardian consent. */
@Service
public class ExhibitionService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final InternalClient client;
    private final PortfolioService portfolios;

    public ExhibitionService(
            JdbcTemplate jdbc,
            ObjectMapper json,
            InternalClient client,
            PortfolioService portfolios) {
        this.jdbc = jdbc;
        this.json = json;
        this.client = client;
        this.portfolios = portfolios;
    }

    @Transactional
    public Map<String, Object> consent(Actor actor, String publicationId, Consent input) {
        Publication publication = portfolios.publication(actor.tenantId(), publicationId);
        if (actor.isStaff()) {
            throw new PlatformException(403, "请切换监护人身份后授权，老师不能代办");
        }
        boolean granted = false;
        for (String student : participants(publication)) {
            Map<?, ?> recipients =
                    client.get(
                            "academic",
                            "/internal/academic/students/" + student + "/recipients",
                            Map.class);
            if (!isGuardian(recipients, actor.tenantId(), actor.userId())) {
                continue;
            }
            jdbc.update(
                    "DELETE FROM portfolio_consent WHERE tenant_id=? AND publication_id=? AND student_id=?",
                    actor.tenantId(),
                    publicationId,
                    student);
            jdbc.update(
                    "INSERT INTO portfolio_consent(tenant_id,publication_id,student_id,guardian_id,allowed,display_name,updated_at) VALUES (?,?,?,?,?,?,?)",
                    actor.tenantId(),
                    publicationId,
                    student,
                    actor.userId(),
                    input.allowed(),
                    input.displayName(),
                    Timestamp.from(Instant.now()));
            granted = true;
        }
        if (!granted) {
            throw new PlatformException(403, "需要有效监护人授权关系");
        }
        return Map.of("publicationId", publicationId, "allowed", input.allowed());
    }

    @Transactional
    public Exhibition create(Actor actor, ExhibitionInput input) {
        staff(actor, true);
        if (input.startsAt() != null
                && input.endsAt() != null
                && !input.endsAt().isAfter(input.startsAt())) {
            throw new PlatformException(400, "展览结束时间必须晚于开始时间");
        }
        if (new HashSet<>(input.publicationIds()).size() != input.publicationIds().size()) {
            throw new PlatformException(400, "展览作品不可重复");
        }
        for (String publicationId : input.publicationIds()) {
            Publication publication = portfolios.publication(actor.tenantId(), publicationId);
            portfolios.record(actor, publication.recordId());
            if (!portfolios.isPublicationVisible(actor.tenantId(), publicationId)) {
                throw new PlatformException(400, "展览只能选择已发布作品");
            }
        }
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        jdbc.update(
                "INSERT INTO portfolio_exhibition(id,tenant_id,title,introduction,exhibition_status,content_json,created_at) VALUES (?,?,?,?,'DRAFT',?,?)",
                id,
                actor.tenantId(),
                input.title(),
                input.introduction(),
                encode(input),
                Timestamp.from(now));
        return new Exhibition(
                id,
                input.title(),
                input.introduction(),
                "DRAFT",
                now,
                input.startsAt(),
                input.endsAt());
    }

    public List<Exhibition> list(Actor actor) {
        staff(actor, false);
        return jdbc.query(
                "SELECT id,title,introduction,exhibition_status,created_at,content_json FROM portfolio_exhibition WHERE tenant_id=? ORDER BY created_at DESC LIMIT 100",
                this::exhibitionRow,
                actor.tenantId());
    }

    @Transactional
    public Exhibition approve(Actor actor, String id) {
        staff(actor, true);
        actor.requirePermission("exhibition:approve");
        Stored stored = stored(id);
        requireManagementAccess(actor, stored);
        for (String publicationId : stored.input().publicationIds()) {
            Publication publication = portfolios.publication(actor.tenantId(), publicationId);
            portfolios.record(actor, publication.recordId());
            if (!permitted(stored.tenantId(), publication)) {
                throw new PlatformException(409, "参展作品尚未取得全部监护人授权或已撤回");
            }
        }
        jdbc.update(
                "UPDATE portfolio_exhibition SET exhibition_status='PUBLISHED',approved_by=? WHERE id=? AND tenant_id=?",
                actor.userId(),
                id,
                actor.tenantId());
        return stored(id).exhibition();
    }

    @Transactional
    public Exhibition withdraw(Actor actor, String id) {
        staff(actor, true);
        actor.requirePermission("exhibition:approve");
        Stored stored = stored(id);
        requireManagementAccess(actor, stored);
        jdbc.update(
                "UPDATE portfolio_exhibition SET exhibition_status='WITHDRAWN' WHERE id=? AND tenant_id=?",
                id,
                actor.tenantId());
        return stored(id).exhibition();
    }

    public List<Exhibition> publicList() {
        return jdbc
                .query(
                        "SELECT id,title,introduction,exhibition_status,created_at,content_json FROM portfolio_exhibition WHERE exhibition_status IN ('PUBLISHED','ARCHIVED') ORDER BY created_at DESC LIMIT 100",
                        this::exhibitionRow)
                .stream()
                .filter(this::publiclyVisible)
                .toList();
    }

    public Map<String, Object> publicDetail(String id) {
        Stored stored = stored(id);
        if (!publiclyVisible(stored.exhibition())) {
            throw new PlatformException(404, "展览未发布或已下架");
        }
        List<Map<String, Object>> works = new ArrayList<>();
        for (String publicationId : stored.input().publicationIds()) {
            Publication publication = portfolios.publication(stored.tenantId(), publicationId);
            if (!permitted(stored.tenantId(), publication)) {
                continue;
            }
            String displayName =
                    jdbc.queryForObject(
                            "SELECT display_name FROM portfolio_consent WHERE tenant_id=? AND publication_id=? AND student_id=?",
                            String.class,
                            stored.tenantId(),
                            publicationId,
                            publication.content().studentId());
            List<Map<String, Object>> artworks =
                    publication.content().artworks().stream()
                            .map(
                                    work ->
                                            Map.<String, Object>of(
                                                    "title",
                                                    work.title(),
                                                    "kind",
                                                    work.kind(),
                                                    "mediaIds",
                                                    work.mediaIds(),
                                                    "story",
                                                    work.story()))
                            .toList();
            works.add(
                    Map.of(
                            "publicationId",
                            publicationId,
                            "displayName",
                            displayName == null ? "小画家" : displayName,
                            "artworks",
                            artworks));
        }
        return Map.of("exhibition", stored.exhibition(), "works", works);
    }

    public Object publicMedia(String exhibitionId, String publicationId, List<String> mediaIds) {
        Stored stored = stored(exhibitionId);
        if (!publiclyVisible(stored.exhibition())
                || !stored.input().publicationIds().contains(publicationId)) {
            throw new PlatformException(404, "公开作品不存在");
        }
        Publication publication = portfolios.publication(stored.tenantId(), publicationId);
        if (!permitted(stored.tenantId(), publication)) {
            throw new PlatformException(403, "作品公开授权已撤回");
        }
        Set<String> allowed = new HashSet<>();
        publication.content().artworks().forEach(work -> allowed.addAll(work.mediaIds()));
        if (!allowed.containsAll(mediaIds)) {
            throw new PlatformException(403, "文件不属于公开作品");
        }
        return client.post(
                "media",
                "/internal/media/public-access",
                Map.of(
                        "tenantId",
                        stored.tenantId(),
                        "publicationId",
                        publicationId,
                        "mediaIds",
                        mediaIds),
                Map.class);
    }

    @Transactional
    public Exhibition archive(Actor actor, String id) {
        staff(actor, true);
        actor.requirePermission("exhibition:approve");
        Stored stored = stored(id);
        requireManagementAccess(actor, stored);
        if (!Set.of("PUBLISHED", "ARCHIVED").contains(stored.exhibition().status())) {
            throw new PlatformException(409, "只能归档已审核发布的展览");
        }
        jdbc.update(
                "UPDATE portfolio_exhibition SET exhibition_status='ARCHIVED' WHERE id=? AND tenant_id=?",
                id,
                actor.tenantId());
        return stored(id).exhibition();
    }

    public Map<String, String> certificate(String exhibitionId, String publicationId) {
        Stored stored = stored(exhibitionId);
        if (!publiclyVisible(stored.exhibition())
                || !stored.input().publicationIds().contains(publicationId)) {
            throw new PlatformException(404, "参展证明暂不可用");
        }
        Publication publication = portfolios.publication(stored.tenantId(), publicationId);
        if (!permitted(stored.tenantId(), publication)) {
            throw new PlatformException(403, "参展作品公开授权已撤回");
        }
        String name =
                jdbc.queryForObject(
                        "SELECT display_name FROM portfolio_consent WHERE tenant_id=? AND publication_id=? AND student_id=?",
                        String.class,
                        stored.tenantId(),
                        publicationId,
                        publication.content().studentId());
        String svg =
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"840\" viewBox=\"0 0 1200 840\"><rect width=\"1200\" height=\"840\" fill=\"#fffaf0\"/><rect x=\"35\" y=\"35\" width=\"1130\" height=\"770\" rx=\"20\" fill=\"none\" stroke=\"#b99254\" stroke-width=\"3\"/><g text-anchor=\"middle\" font-family=\"sans-serif\" fill=\"#284b43\"><text x=\"600\" y=\"190\" font-size=\"56\">作品参展证书</text><text x=\"600\" y=\"350\" font-size=\"48\">"
                        + escape(name)
                        + "</text><text x=\"600\" y=\"460\" font-size=\"28\">参与「"
                        + escape(shortText(stored.exhibition().title(), 28))
                        + "」作品展览</text><text x=\"600\" y=\"555\" font-size=\"24\">每一次创作，都值得被看见</text><text x=\"600\" y=\"690\" font-size=\"16\">编号 "
                        + escape(publicationId)
                        + "</text></g></svg>";
        return Map.of("svg", svg, "filename", "exhibition-certificate-" + publicationId + ".svg");
    }

    public Map<String, String> shareCard(String exhibitionId) {
        Stored stored = stored(exhibitionId);
        Map<String, Object> detail = publicDetail(exhibitionId);
        int count = ((List<?>) detail.get("works")).size();
        String svg =
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1000\" height=\"800\" viewBox=\"0 0 1000 800\"><rect width=\"1000\" height=\"800\" rx=\"35\" fill=\"#eaf4f0\"/><circle cx=\"820\" cy=\"90\" r=\"170\" fill=\"#eedbb8\"/><g font-family=\"sans-serif\" fill=\"#244d45\"><text x=\"80\" y=\"210\" font-size=\"28\">学生作品线上展览</text><text x=\"80\" y=\"340\" font-size=\"44\">"
                        + escape(shortText(stored.exhibition().title(), 20))
                        + "</text><text x=\"80\" y=\"435\" font-size=\"24\">"
                        + count
                        + " 份创作，记录成长中的想象力</text><text x=\"80\" y=\"620\" font-size=\"28\">走进画室 · 看见成长</text><text x=\"80\" y=\"710\" font-size=\"16\">"
                        + escape(exhibitionId)
                        + "</text></g></svg>";
        return Map.of("svg", svg, "filename", "exhibition-share-" + exhibitionId + ".svg");
    }

    private Exhibition exhibitionRow(java.sql.ResultSet rs, int index)
            throws java.sql.SQLException {
        ExhibitionInput input = decode(rs.getString("content_json"));
        String status = rs.getString("exhibition_status");
        if ("PUBLISHED".equals(status)
                && input.endsAt() != null
                && !input.endsAt().isAfter(Instant.now())) {
            status = "ARCHIVED";
        }
        return new Exhibition(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("introduction"),
                status,
                rs.getTimestamp("created_at").toInstant(),
                input.startsAt(),
                input.endsAt());
    }

    private boolean publiclyVisible(Exhibition exhibition) {
        return Set.of("PUBLISHED", "ARCHIVED").contains(exhibition.status())
                && (exhibition.startsAt() == null || !exhibition.startsAt().isAfter(Instant.now()));
    }

    private String escape(String value) {
        return value == null
                ? ""
                : value.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&apos;");
    }

    private String shortText(String value, int maximum) {
        return value.codePointCount(0, value.length()) > maximum
                ? value.substring(0, value.offsetByCodePoints(0, maximum)) + "…"
                : value;
    }

    private boolean permitted(String tenantId, Publication publication) {
        if (!portfolios.isPublicationVisible(tenantId, publication.id())) {
            return false;
        }
        for (String student : participants(publication)) {
            List<String> guardians =
                    jdbc.query(
                            "SELECT guardian_id FROM portfolio_consent WHERE tenant_id=? AND publication_id=? AND student_id=? AND allowed=TRUE",
                            (rs, n) -> rs.getString(1),
                            tenantId,
                            publication.id(),
                            student);
            if (guardians.isEmpty()) {
                return false;
            }
            Map<?, ?> recipients =
                    client.get(
                            "academic",
                            "/internal/academic/students/" + student + "/recipients",
                            Map.class);
            if (!isGuardian(recipients, tenantId, guardians.get(0))) {
                return false;
            }
        }
        return true;
    }

    private boolean isGuardian(Map<?, ?> response, String tenantId, String userId) {
        if (response == null
                || !tenantId.equals(String.valueOf(response.get("tenantId")))
                || !(response.get("recipients") instanceof List<?> list)) {
            return false;
        }
        return list.stream()
                .anyMatch(
                        value ->
                                value instanceof Map<?, ?> row
                                        && userId.equals(String.valueOf(row.get("userId"))));
    }

    private Set<String> participants(Publication publication) {
        Set<String> ids = new LinkedHashSet<>();
        ids.add(publication.content().studentId());
        for (Artwork artwork : publication.content().artworks()) {
            ids.addAll(artwork.participantIds());
        }
        return ids;
    }

    private Stored stored(String id) {
        List<Stored> rows =
                jdbc.query(
                        "SELECT tenant_id,id,title,introduction,exhibition_status,content_json,created_at FROM portfolio_exhibition WHERE id=?",
                        (rs, n) ->
                                new Stored(
                                        rs.getString("tenant_id"),
                                        exhibitionRow(rs, n),
                                        decode(rs.getString("content_json"))),
                        id);
        if (rows.isEmpty()) {
            throw new PlatformException(404, "展览不存在");
        }
        return rows.get(0);
    }

    private void requireManagementAccess(Actor actor, Stored stored) {
        requireTenant(actor, stored);
        for (String publicationId : stored.input().publicationIds()) {
            Publication publication = portfolios.publication(actor.tenantId(), publicationId);
            portfolios.record(actor, publication.recordId());
        }
    }

    private void requireTenant(Actor actor, Stored stored) {
        if (!actor.tenantId().equals(stored.tenantId())) {
            throw new PlatformException(404, "展览不存在");
        }
    }

    private void staff(Actor actor, boolean write) {
        if (!actor.isStaff()) {
            throw new PlatformException(403, "需要员工权限");
        }
        if (!write || !actor.permissions().contains("portfolio:write")) {
            actor.requirePermission(write ? "student:write" : "student:read");
        }
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("展览内容无效", e);
        }
    }

    private ExhibitionInput decode(String value) {
        try {
            return json.readValue(value, ExhibitionInput.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("展览内容无效", e);
        }
    }

    private record Stored(String tenantId, Exhibition exhibition, ExhibitionInput input) {}
}
