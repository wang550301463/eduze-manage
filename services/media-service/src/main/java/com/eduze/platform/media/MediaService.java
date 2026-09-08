package com.eduze.platform.media;

import com.eduze.platform.runtime.*;
import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaService {
    public record UploadRequest(
            @NotBlank String branchId,
            @NotBlank String purpose,
            @NotBlank @Size(max = 255) String fileName,
            @NotBlank String contentType,
            @Positive long size) {}

    public record UploadResult(
            String id,
            String uploadUrl,
            String method,
            Map<String, String> headers,
            Instant expiresAt) {}

    public record Usable(boolean usable, String tenantId, String ownerId) {}

    public record File(
            String id,
            String tenantId,
            String branchId,
            String ownerId,
            String fileName,
            String purpose,
            String contentType,
            long size,
            String objectKey,
            String status,
            Instant expiresAt) {}

    public record References(
            @NotBlank String ownerType,
            @NotBlank String ownerId,
            @NotNull @Size(max = 100) List<String> mediaIds) {}

    public record Access(@NotNull @Size(max = 100) List<String> mediaIds) {}

    public record PublicAccess(
            @NotBlank String tenantId,
            @NotBlank String publicationId,
            @NotNull @Size(max = 100) List<String> mediaIds) {}

    public record Link(
            String id,
            String url,
            Instant expiresAt,
            String contentType,
            @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    String thumbnailUrl) {}

    public record Links(List<Link> items) {}

    private final JdbcTemplate jdbc;
    private final ObjectStore store;

    public MediaService(JdbcTemplate jdbc, ObjectStore store) {
        this.jdbc = jdbc;
        this.store = store;
    }

    public UploadResult create(UploadRequest request) {
        Actor actor = Actors.current();
        if (!actor.isStaff()) {
            throw new PlatformException(403, "仅老师可上传教学媒体");
        }
        actor.requireBranch(request.branchId());
        if (!Set.of("ARTWORK", "COURSEWARE", "AUDIO").contains(request.purpose())) {
            throw new PlatformException(400, "上传用途无效");
        }
        actor.requirePermission(
                request.purpose().equals("COURSEWARE") ? "course:write" : "student:write");
        MediaRules.validateUpload(request.contentType(), request.size());
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now(), expiry = now.plusSeconds(900);
        String key =
                "staging/"
                        + actor.tenantId()
                        + "/"
                        + request.branchId()
                        + "/"
                        + id
                        + "."
                        + MediaRules.extension(request.contentType());
        ObjectStore.Upload upload = store.upload(id, key, request.contentType(), expiry);
        jdbc.update(
                "INSERT INTO media_file(id,tenant_id,branch_id,owner_id,file_name,purpose,content_type,file_size,object_key,status,created_at,expires_at) VALUES(?,?,?,?,?,?,?,?,?,'PENDING',?,?)",
                id,
                actor.tenantId(),
                request.branchId(),
                actor.userId(),
                request.fileName(),
                request.purpose(),
                request.contentType(),
                request.size(),
                key,
                Timestamp.from(now),
                Timestamp.from(expiry));
        return new UploadResult(id, upload.url(), upload.method(), upload.headers(), expiry);
    }

    public File find(String id) {
        return jdbc
                .query(
                        "SELECT id,tenant_id,branch_id,owner_id,file_name,purpose,content_type,file_size,object_key,status,expires_at FROM media_file WHERE id=?",
                        (rs, n) ->
                                new File(
                                        rs.getString(1),
                                        rs.getString(2),
                                        rs.getString(3),
                                        rs.getString(4),
                                        rs.getString(5),
                                        rs.getString(6),
                                        rs.getString(7),
                                        rs.getLong(8),
                                        rs.getString(9),
                                        rs.getString(10),
                                        rs.getTimestamp(11).toInstant()),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new PlatformException(404, "文件不存在"));
    }

    private void owner(File file) {
        Actor actor = Actors.current();
        if (!file.tenantId().equals(actor.tenantId()) || !file.ownerId().equals(actor.userId())) {
            throw new PlatformException(403, "无权确认该文件");
        }
        actor.requireBranch(file.branchId());
    }

    @Transactional
    public Map<String, String> complete(String id) {
        jdbc.queryForList("SELECT id FROM media_file WHERE id=? FOR UPDATE", String.class, id);
        File file = find(id);
        owner(file);
        if (file.status().equals("READY")) {
            return Map.of("id", id, "status", "READY");
        }
        if (!file.status().equals("PENDING")) {
            throw new PlatformException(409, "文件状态不允许确认");
        }
        if (file.expiresAt().isBefore(Instant.now())) {
            throw new PlatformException(409, "上传凭证已过期，请重新上传");
        }
        String sealedKey =
                file.tenantId()
                        + "/sealed/"
                        + UUID.randomUUID()
                        + "."
                        + MediaRules.extension(file.contentType());
        store.seal(file.objectKey(), sealedKey);
        try {
            ObjectStore.Metadata metadata = store.inspect(sealedKey);
            if (metadata.size() != file.size()
                    || !file.contentType().equals(metadata.contentType())) {
                throw new PlatformException(422, "上传文件与声明不一致");
            }
            MediaRules.validateContent(file.contentType(), metadata.signature());
            jdbc.update(
                    "UPDATE media_file SET status='READY',staging_key=object_key,object_key=?,last_accessed_at=CURRENT_TIMESTAMP,access_version=access_version+1 WHERE id=? AND status='PENDING'",
                    sealedKey,
                    id);
        } catch (RuntimeException error) {
            store.delete(sealedKey);
            throw error;
        }
        return Map.of("id", id, "status", "READY");
    }

    @Transactional
    public Usable usable(String id) {
        File file = find(id);
        Actor actor = Actors.current();
        if (!file.tenantId().equals(actor.tenantId())) {
            throw new PlatformException(403, "无权访问文件");
        }
        if (!actor.isStaff()) {
            throw new PlatformException(403, "无权关联文件");
        }
        actor.requireBranch(file.branchId());
        boolean ready = file.status().equals("READY") && touchReady(id, actor.tenantId());
        return new Usable(ready, file.tenantId(), file.ownerId());
    }

    @Transactional
    public void references(String caller, References request) {
        Actor actor = Actors.current();
        if (!Set.of("teaching", "portfolio", "engagement", "commerce").contains(caller)) {
            throw new PlatformException(403, "无权关联媒体");
        }
        for (String id : new HashSet<>(request.mediaIds())) {
            if (!usable(id).usable()) {
                throw new PlatformException(409, "文件尚未可用");
            }
        }
        jdbc.update(
                "DELETE FROM media_reference WHERE tenant_id=? AND caller=? AND owner_type=? AND owner_id=?",
                actor.tenantId(),
                caller,
                request.ownerType(),
                request.ownerId());
        for (String id : new HashSet<>(request.mediaIds())) {
            jdbc.update(
                    "INSERT INTO media_reference(tenant_id,caller,owner_type,owner_id,media_id) VALUES(?,?,?,?,?)",
                    actor.tenantId(),
                    caller,
                    request.ownerType(),
                    request.ownerId(),
                    id);
        }
    }

    @Transactional
    public Links access(String caller, Access request) {
        if (!Set.of("teaching", "portfolio", "engagement", "commerce").contains(caller)) {
            throw new PlatformException(403, "无权读取媒体");
        }
        Actor actor = Actors.current();
        List<Link> links = new ArrayList<>();
        Instant expiry = Instant.now().plusSeconds(300);
        for (String id : new LinkedHashSet<>(request.mediaIds())) {
            File file = find(id);
            if (!actor.tenantId().equals(file.tenantId()) || !file.status().equals("READY")) {
                throw new PlatformException(403, "文件不可访问");
            }
            // The owning service has performed its live household / resource authorization.
            touchReady(id, file.tenantId());
            links.add(
                    new Link(
                            id,
                            store.readUrl(id, file.objectKey(), expiry, false),
                            expiry,
                            file.contentType(),
                            file.contentType().startsWith("image/")
                                    ? store.readUrl(id, file.objectKey(), expiry, true)
                                    : null));
        }
        return new Links(links);
    }

    @Transactional
    public Links publicAccess(String caller, PublicAccess request) {
        if (!caller.equals("portfolio")) {
            throw new PlatformException(403, "无权发布公开媒体");
        }
        List<Link> links = new ArrayList<>();
        Instant expiry = Instant.now().plusSeconds(300);
        for (String id : new LinkedHashSet<>(request.mediaIds())) {
            File file = find(id);
            Integer count =
                    jdbc.queryForObject(
                            "SELECT COUNT(*) FROM media_reference WHERE tenant_id=? AND caller='portfolio' AND owner_id=? AND media_id=?",
                            Integer.class,
                            request.tenantId(),
                            request.publicationId(),
                            id);
            if (!file.tenantId().equals(request.tenantId())
                    || !file.status().equals("READY")
                    || count == null
                    || count == 0) {
                throw new PlatformException(403, "作品不包含该文件");
            }
            touchReady(id, file.tenantId());
            links.add(
                    new Link(
                            id,
                            store.readUrl(id, file.objectKey(), expiry, false),
                            expiry,
                            file.contentType(),
                            file.contentType().startsWith("image/")
                                    ? store.readUrl(id, file.objectKey(), expiry, true)
                                    : null));
        }
        return new Links(links);
    }

    /** Atomically invalidates any GC review snapshot when a file is used or referenced. */
    public boolean touchReady(String id, String tenantId) {
        return jdbc.update(
                        "UPDATE media_file SET last_accessed_at=CURRENT_TIMESTAMP,access_version=access_version+1 WHERE id=? AND tenant_id=? AND status='READY'",
                        id,
                        tenantId)
                == 1;
    }
}
