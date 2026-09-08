ALTER TABLE portfolio_record ADD COLUMN needs_publishing BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE portfolio_record SET needs_publishing=FALSE WHERE publication_status='PUBLISHED';
CREATE TABLE portfolio_entry (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, record_id VARCHAR(64) NOT NULL,
 student_id VARCHAR(64) NOT NULL, lesson_id VARCHAR(64) NOT NULL, entry_status VARCHAR(30) NOT NULL,
 row_version INT NOT NULL, command_key VARCHAR(100) NOT NULL, content_json LONGTEXT NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 UNIQUE KEY uk_entry_command (tenant_id,record_id,command_key),
 INDEX idx_entry_student (tenant_id,student_id,entry_status,created_at), INDEX idx_entry_record (tenant_id,record_id,created_at)
);
CREATE TABLE portfolio_collection (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, student_id VARCHAR(64) NOT NULL,
 content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_collection_student (tenant_id,student_id,created_at)
);
