package com.eduze.platform.media;

import com.eduze.platform.runtime.EventEnvelope;
import com.eduze.platform.runtime.EventHandler;
import com.eduze.platform.runtime.PlatformException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** References follow committed business state; stale events cannot remove newer references. */
@Component
public class MediaReferenceEvents implements EventHandler {
    private final JdbcTemplate jdbc;
    private final MediaService media;

    public MediaReferenceEvents(JdbcTemplate jdbc, MediaService media) {
        this.jdbc = jdbc;
        this.media = media;
    }

    public boolean supports(String type) {
        return Set.of(
                        "teaching.media-references",
                        "portfolio.media-references",
                        "engagement.media-references",
                        "commerce.media-references")
                .contains(type);
    }

    public void handle(EventEnvelope event) {
        String caller = event.type().substring(0, event.type().indexOf('.'));
        var payload = event.payload();
        String ownerType = payload.path("ownerType").asText();
        String ownerId = payload.path("ownerId").asText();
        long revision = payload.path("revision").asLong(-1);
        if (ownerType.isBlank()
                || ownerId.isBlank()
                || revision < 0
                || !payload.path("mediaIds").isArray()
                || payload.path("mediaIds").size() > 100) {
            throw new PlatformException(400, "媒体引用事件无效");
        }
        try {
            jdbc.update(
                    "INSERT INTO media_reference_cursor(tenant_id,caller,owner_type,owner_id,revision) VALUES(?,?,?,?,-1)",
                    event.tenantId(),
                    caller,
                    ownerType,
                    ownerId);
        } catch (DuplicateKeyException duplicate) {
            /* Current read obtains the row lock below. */
        }
        Long current =
                jdbc.queryForObject(
                        "SELECT revision FROM media_reference_cursor WHERE tenant_id=? AND caller=? AND owner_type=? AND owner_id=? FOR UPDATE",
                        Long.class,
                        event.tenantId(),
                        caller,
                        ownerType,
                        ownerId);
        if (current != null && current >= revision) {
            return;
        }
        Set<String> ids = new LinkedHashSet<>();
        payload.get("mediaIds").forEach(id -> ids.add(id.asText()));
        for (String id : ids) {
            var file = media.find(id);
            if (!file.tenantId().equals(event.tenantId()) || !file.status().equals("READY")) {
                throw new PlatformException(409, "引用文件尚未可用");
            }
        }
        for (String id : ids) {
            media.touchReady(id, event.tenantId());
        }
        jdbc.update(
                "DELETE FROM media_reference WHERE tenant_id=? AND caller=? AND owner_type=? AND owner_id=?",
                event.tenantId(),
                caller,
                ownerType,
                ownerId);
        for (String id : ids) {
            jdbc.update(
                    "INSERT INTO media_reference(tenant_id,caller,owner_type,owner_id,media_id) VALUES(?,?,?,?,?)",
                    event.tenantId(),
                    caller,
                    ownerType,
                    ownerId,
                    id);
        }
        jdbc.update(
                "UPDATE media_reference_cursor SET revision=? WHERE tenant_id=? AND caller=? AND owner_type=? AND owner_id=?",
                revision,
                event.tenantId(),
                caller,
                ownerType,
                ownerId);
    }
}
