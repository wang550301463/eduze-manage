CREATE TABLE portfolio_record (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, branch_id VARCHAR(64) NOT NULL,
 theme_id VARCHAR(64) NOT NULL, student_id VARCHAR(64) NOT NULL, row_version INT NOT NULL,
 progress_status VARCHAR(30) NOT NULL, publication_status VARCHAR(30) NOT NULL,
 current_publication_id VARCHAR(64), content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 UNIQUE KEY uk_record_student_theme (tenant_id,theme_id,student_id), INDEX idx_record_student (tenant_id,student_id)
);
CREATE TABLE portfolio_publication (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, record_id VARCHAR(64) NOT NULL,
 student_id VARCHAR(64) NOT NULL, version_number INT NOT NULL, revoked BOOLEAN NOT NULL DEFAULT FALSE, idempotency_key VARCHAR(100) NOT NULL,
 content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 UNIQUE KEY uk_publication_command (tenant_id,record_id,idempotency_key), INDEX idx_publication_record (tenant_id,record_id)
);
CREATE TABLE portfolio_audit (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, record_id VARCHAR(64) NOT NULL,
 actor_id VARCHAR(64) NOT NULL, action_name VARCHAR(30) NOT NULL, reason VARCHAR(2000) NOT NULL,
 content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_portfolio_audit (tenant_id,record_id)
);
CREATE TABLE portfolio_assessment (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, student_id VARCHAR(64) NOT NULL,
 branch_id VARCHAR(64) NOT NULL, content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_assessment_student (tenant_id,student_id)
);
CREATE TABLE portfolio_report (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, student_id VARCHAR(64) NOT NULL,
 content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_report_student (tenant_id,student_id)
);
CREATE TABLE portfolio_consent (
 tenant_id VARCHAR(64) NOT NULL, publication_id VARCHAR(64) NOT NULL, student_id VARCHAR(64) NOT NULL,
 guardian_id VARCHAR(64) NOT NULL, allowed BOOLEAN NOT NULL, display_name VARCHAR(100) NOT NULL, updated_at TIMESTAMP(6) NOT NULL,
 PRIMARY KEY (tenant_id,publication_id,student_id)
);
CREATE TABLE portfolio_exhibition (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, title VARCHAR(200) NOT NULL,
 introduction VARCHAR(10000) NOT NULL, exhibition_status VARCHAR(30) NOT NULL,
 approved_by VARCHAR(64), content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_exhibition_status (tenant_id,exhibition_status)
);
