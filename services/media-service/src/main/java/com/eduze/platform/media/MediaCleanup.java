package com.eduze.platform.media;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MediaCleanup {
    private final JdbcTemplate jdbc;
    private final ObjectStore store;

    public MediaCleanup(JdbcTemplate jdbc, ObjectStore store) {
        this.jdbc = jdbc;
        this.store = store;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${eduze.media.cleanup-delay-ms:3600000}")
    public void cleanup() {
        Timestamp cutoff = Timestamp.from(Instant.now().minusSeconds(86400));
        var ids =
                jdbc.queryForList(
                        "SELECT id FROM media_file WHERE expires_at < ? AND (status IN ('PENDING','FAILED') OR staging_key IS NOT NULL) ORDER BY id LIMIT 50",
                        String.class,
                        cutoff);
        for (String id : ids) {
            // Completion uses the same row lock; reread after acquiring it before any object
            // deletion.
            var rows =
                    jdbc.queryForList(
                            "SELECT status,object_key,staging_key FROM media_file WHERE id=? AND expires_at < ? FOR UPDATE",
                            id,
                            cutoff);
            if (rows.isEmpty()) continue;
            var row = rows.get(0);
            String status = (String) row.get("status");
            if (status.equals("PENDING") || status.equals("FAILED")) {
                Integer references =
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM media_reference WHERE media_id=?",
                                Integer.class,
                                id);
                if (references != null && references > 0) continue;
                String key = (String) row.get("object_key");
                if (!key.startsWith("staging/")) continue;
                store.delete(key);
                jdbc.update("DELETE FROM media_file WHERE id=?", id);
            } else if (status.equals("READY")) {
                String staging = (String) row.get("staging_key");
                // Sealed objects remain intact even when reference delivery is delayed
                // indefinitely.
                if (staging != null
                        && staging.startsWith("staging/")
                        && !staging.equals(row.get("object_key"))) {
                    store.delete(staging);
                    jdbc.update("UPDATE media_file SET staging_key=NULL WHERE id=?", id);
                }
            }
        }
    }
}
