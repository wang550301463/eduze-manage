CREATE TABLE media_file (
 id VARCHAR(36) PRIMARY KEY,tenant_id VARCHAR(36) NOT NULL,branch_id VARCHAR(36) NOT NULL,
 owner_id VARCHAR(36) NOT NULL,file_name VARCHAR(255) NOT NULL,purpose VARCHAR(32) NOT NULL,
 content_type VARCHAR(128) NOT NULL,file_size BIGINT NOT NULL,object_key VARCHAR(512) NOT NULL,
 status VARCHAR(20) NOT NULL,created_at DATETIME(6) NOT NULL,expires_at DATETIME(6) NOT NULL,
 KEY idx_media_owner(tenant_id,owner_id,created_at),KEY idx_media_cleanup(status,expires_at)
) COMMENT='Private media metadata; object URLs are never persisted';
CREATE TABLE media_reference (
 tenant_id VARCHAR(36) NOT NULL,caller VARCHAR(40) NOT NULL,owner_type VARCHAR(40) NOT NULL,
 owner_id VARCHAR(36) NOT NULL,media_id VARCHAR(36) NOT NULL,
 PRIMARY KEY(tenant_id,caller,owner_type,owner_id,media_id),KEY idx_media_reference(media_id)
) COMMENT='Owning service references protect immutable publication files';
