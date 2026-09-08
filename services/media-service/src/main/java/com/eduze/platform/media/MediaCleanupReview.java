package com.eduze.platform.media;

import com.eduze.platform.runtime.*;
import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Advisory orphan review only: absent async references do not establish business ownership. */
@Service
public class MediaCleanupReview {
    public record Candidate(
            String id, String branchId, String fileName, long version, Instant lastAccessedAt) {}

    public record Review(
            @PositiveOrZero long expectedVersion,
            @NotBlank @Pattern(regexp = "RETAIN|DELETE") String decision,
            @NotBlank @Size(max = 1000) String reason) {}

    public record Result(String id, String decision, boolean deleted, long version) {}

    private static final String ELIGIBLE =
            "status='READY' AND COALESCE(last_accessed_at,created_at) < ? AND NOT EXISTS(SELECT 1 FROM media_reference r WHERE r.media_id=media_file.id)";
    private final JdbcTemplate jdbc;

    public MediaCleanupReview(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private Actor reviewer() {
        Actor actor = Actors.current();
        if (!actor.isStaff()) throw new PlatformException(403, "无权审核媒体清理");
        actor.requirePermission("media:manage");
        return actor;
    }

    private Timestamp cutoff() {
        return Timestamp.from(Instant.now().minusSeconds(30 * 86400L));
    }

    public List<Candidate> candidates() {
        Actor actor = reviewer();
        if (!actor.isSuperAdmin() && actor.branchIds().isEmpty()) return List.of();
        List<Object> parameters = new ArrayList<>(List.of(actor.tenantId(), cutoff()));
        String branchScope = "";
        if (!actor.isSuperAdmin()) {
            branchScope =
                    " AND branch_id IN ("
                            + String.join(",", Collections.nCopies(actor.branchIds().size(), "?"))
                            + ")";
            parameters.addAll(actor.branchIds());
        }
        return jdbc.query(
                "SELECT id,branch_id,file_name,access_version,COALESCE(last_accessed_at,created_at) FROM media_file WHERE tenant_id=? AND "
                        + ELIGIBLE
                        + branchScope
                        + " ORDER BY id LIMIT 100",
                (rs, n) ->
                        new Candidate(
                                rs.getString(1),
                                rs.getString(2),
                                rs.getString(3),
                                rs.getLong(4),
                                rs.getTimestamp(5).toInstant()),
                parameters.toArray());
    }

    @Transactional
    public Result review(String id, Review review) {
        Actor actor = reviewer();
        if (review.decision() == null
                || !Set.of("RETAIN", "DELETE").contains(review.decision())
                || review.reason() == null
                || review.reason().isBlank()
                || review.reason().length() > 1000
                || review.expectedVersion() < 0) {
            throw new PlatformException(400, "审核参数无效");
        }
        var branches =
                jdbc.queryForList(
                        "SELECT branch_id FROM media_file WHERE id=? AND tenant_id=?",
                        String.class,
                        id,
                        actor.tenantId());
        if (branches.isEmpty()) throw new PlatformException(404, "文件不存在");
        actor.requireBranch(branches.get(0));
        // Compare-and-set competes atomically with usable/access/reference touches.
        int changed =
                jdbc.update(
                        "UPDATE media_file SET last_accessed_at=CURRENT_TIMESTAMP,access_version=access_version+1 WHERE id=? AND tenant_id=? AND access_version=? AND "
                                + ELIGIBLE,
                        id,
                        actor.tenantId(),
                        review.expectedVersion(),
                        cutoff());
        if (changed != 1) throw new PlatformException(409, "审核已过期、文件已被使用或存在引用，请刷新候选");
        String decision =
                review.decision().equals("DELETE") ? "BLOCKED_OWNER_PROTOCOL_REQUIRED" : "RETAIN";
        jdbc.update(
                "INSERT INTO media_cleanup_audit(id,media_id,tenant_id,actor_id,expected_version,decision,reason,reviewed_at) VALUES(?,?,?,?,?,?,?,?)",
                UUID.randomUUID().toString(),
                id,
                actor.tenantId(),
                actor.userId(),
                review.expectedVersion(),
                decision,
                review.reason(),
                Timestamp.from(Instant.now()));
        return new Result(id, decision, false, review.expectedVersion() + 1);
    }
}
