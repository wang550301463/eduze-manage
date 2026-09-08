package com.eduze.platform.media;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class MediaMigrationTest {
    @Test
    void cleanupMigrationPreservesSealedFilesAndInitializesReviewVersions() {
        var ds =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        var jdbc = new JdbcTemplate(ds);
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/V1__media.sql"),
                        new ClassPathResource("db/migration/V2__reference_cursor.sql"))
                .execute(ds);
        jdbc.update(
                "INSERT INTO media_file VALUES('existing','tenant','branch','owner','x.jpg','ARTWORK','image/jpeg',4,'tenant/sealed/key','READY',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/V3__safe_cleanup_review.sql"))
                .execute(ds);
        assertEquals(
                "tenant/sealed/key",
                jdbc.queryForObject("SELECT object_key FROM media_file", String.class));
        assertNull(jdbc.queryForObject("SELECT staging_key FROM media_file", String.class));
        assertEquals(0L, jdbc.queryForObject("SELECT access_version FROM media_file", Long.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM media_file WHERE last_accessed_at=created_at",
                        Integer.class));
    }
}
