ALTER TABLE media_file ADD COLUMN staging_key VARCHAR(512) NULL;
ALTER TABLE media_file ADD COLUMN last_accessed_at DATETIME(6) NULL;
ALTER TABLE media_file ADD COLUMN access_version BIGINT NOT NULL DEFAULT 0;
UPDATE media_file SET staging_key=object_key WHERE status IN ('PENDING','FAILED') AND object_key LIKE 'staging/%';
UPDATE media_file SET last_accessed_at=created_at;
CREATE INDEX idx_media_review ON media_file(status,last_accessed_at);
CREATE TABLE media_cleanup_audit (
 id VARCHAR(36) PRIMARY KEY,media_id VARCHAR(36) NOT NULL,tenant_id VARCHAR(36) NOT NULL,
 actor_id VARCHAR(36) NOT NULL,expected_version BIGINT NOT NULL,decision VARCHAR(32) NOT NULL,
 reason VARCHAR(1000) NOT NULL,reviewed_at DATETIME(6) NOT NULL,
 KEY idx_media_audit(tenant_id,media_id,reviewed_at)
) COMMENT='Versioned retention reviews; READY physical deletion is intentionally blocked';
