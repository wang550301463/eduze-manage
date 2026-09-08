CREATE TABLE media_reference_cursor (
 tenant_id VARCHAR(36) NOT NULL,
 caller VARCHAR(40) NOT NULL,
 owner_type VARCHAR(40) NOT NULL,
 owner_id VARCHAR(36) NOT NULL,
 revision BIGINT NOT NULL,
 PRIMARY KEY(tenant_id,caller,owner_type,owner_id)
) COMMENT='Per-owner version prevents stale reference events replacing current references';
