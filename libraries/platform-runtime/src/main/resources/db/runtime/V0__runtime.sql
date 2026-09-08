CREATE TABLE platform_outbox (
 event_id VARCHAR(36) NOT NULL PRIMARY KEY,
 target VARCHAR(40) NOT NULL,
 envelope LONGTEXT NOT NULL,
 status VARCHAR(20) NOT NULL,
 attempts INT NOT NULL DEFAULT 0,
 next_attempt_at DATETIME(6) NOT NULL,
 lease_until DATETIME(6),
 created_at DATETIME(6) NOT NULL,
 KEY idx_outbox_due(status,next_attempt_at),
 KEY idx_outbox_lease(status,lease_until)
) COMMENT='Service-local transactional event delivery';
CREATE TABLE platform_inbox (
 event_id VARCHAR(36) NOT NULL PRIMARY KEY,
 event_type VARCHAR(80) NOT NULL,
 processed_at DATETIME(6) NOT NULL
) COMMENT='Service-local atomic consumer deduplication';
