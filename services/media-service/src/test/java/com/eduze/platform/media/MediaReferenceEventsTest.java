package com.eduze.platform.media;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.platform.runtime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

class MediaReferenceEventsTest {
    @Test
    void staleReferenceCannotUndoNewerPublicationAndFailedEventCanRetry() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:media-events;MODE=MySQL", "sa", "");
        // Keep a connection open for this database's lifetime.
        try (var connection = ds.getConnection()) {
            var jdbc = new JdbcTemplate(ds);
            jdbc.execute(
                    "CREATE TABLE platform_inbox(event_id VARCHAR(36) PRIMARY KEY,event_type VARCHAR(80),processed_at TIMESTAMP)");
            jdbc.execute(
                    "CREATE TABLE media_reference_cursor(tenant_id VARCHAR(36),caller VARCHAR(40),owner_type VARCHAR(40),owner_id VARCHAR(36),revision BIGINT,PRIMARY KEY(tenant_id,caller,owner_type,owner_id))");
            jdbc.execute(
                    "CREATE TABLE media_reference(tenant_id VARCHAR(36),caller VARCHAR(40),owner_type VARCHAR(40),owner_id VARCHAR(36),media_id VARCHAR(36),PRIMARY KEY(tenant_id,caller,owner_type,owner_id,media_id))");
            var media = mock(MediaService.class);
            when(media.find("new"))
                    .thenReturn(
                            new MediaService.File(
                                    "new",
                                    "1",
                                    "2",
                                    "teacher",
                                    "x.jpg",
                                    "ARTWORK",
                                    "image/jpeg",
                                    4,
                                    "key",
                                    "READY",
                                    Instant.now()));
            var handler = new MediaReferenceEvents(jdbc, media);
            var inbox =
                    new Inbox(
                            jdbc,
                            new TransactionTemplate(new DataSourceTransactionManager(ds)),
                            List.of(handler));
            assertTrue(inbox.receive(event("new-event", 2, List.of("new"))));
            assertTrue(inbox.receive(event("old-event", 1, List.of())));
            assertFalse(inbox.receive(event("new-event", 2, List.of("new"))));
            assertEquals(
                    List.of("new"),
                    jdbc.queryForList("SELECT media_id FROM media_reference", String.class));
            when(media.find("missing")).thenThrow(new PlatformException(404, "文件不存在"));
            assertThrows(
                    PlatformException.class,
                    () -> inbox.receive(event("retry-event", 3, List.of("missing"))));
            assertEquals(
                    2, jdbc.queryForObject("SELECT COUNT(*) FROM platform_inbox", Integer.class));
            assertEquals(
                    2,
                    jdbc.queryForObject(
                            "SELECT revision FROM media_reference_cursor", Integer.class));
            assertEquals(
                    List.of("new"),
                    jdbc.queryForList("SELECT media_id FROM media_reference", String.class));
            doReturn(
                            new MediaService.File(
                                    "missing",
                                    "1",
                                    "2",
                                    "teacher",
                                    "x.jpg",
                                    "ARTWORK",
                                    "image/jpeg",
                                    4,
                                    "key",
                                    "READY",
                                    Instant.now()))
                    .when(media)
                    .find("missing");
            assertTrue(inbox.receive(event("retry-event", 3, List.of("missing"))));
            assertEquals(
                    List.of("missing"),
                    jdbc.queryForList("SELECT media_id FROM media_reference", String.class));
        } catch (java.sql.SQLException error) {
            throw new AssertionError(error);
        }
    }

    private EventEnvelope event(String id, int revision, List<String> ids) {
        return new EventEnvelope(
                id,
                "portfolio.media-references",
                1,
                "1",
                "2",
                "publication",
                Instant.now(),
                "trace",
                new ObjectMapper()
                        .valueToTree(
                                Map.of(
                                        "ownerType",
                                        "publication",
                                        "ownerId",
                                        "publication",
                                        "revision",
                                        revision,
                                        "mediaIds",
                                        ids)));
    }
}
