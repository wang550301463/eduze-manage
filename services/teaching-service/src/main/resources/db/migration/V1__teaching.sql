CREATE TABLE teaching_template (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, owner_id VARCHAR(64) NOT NULL,
 row_version INT NOT NULL, title VARCHAR(200) NOT NULL, content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_template_owner (tenant_id, owner_id)
);
CREATE TABLE teaching_template_version (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, template_id VARCHAR(64) NOT NULL,
 version_number INT NOT NULL, title VARCHAR(200) NOT NULL, content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 UNIQUE KEY uk_template_version (tenant_id, template_id, version_number)
);
CREATE TABLE teaching_resource (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, owner_id VARCHAR(64) NOT NULL,
 row_version INT NOT NULL, published BOOLEAN NOT NULL, title VARCHAR(200) NOT NULL, content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_resource_visibility (tenant_id, owner_id, published)
);
CREATE TABLE teaching_theme (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, branch_id VARCHAR(64) NOT NULL, group_id VARCHAR(64) NOT NULL,
 row_version INT NOT NULL, theme_status VARCHAR(30) NOT NULL, content_json LONGTEXT NOT NULL,
 template_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_theme_branch (tenant_id, branch_id, group_id)
);
CREATE TABLE teaching_theme_audit (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, theme_id VARCHAR(64) NOT NULL,
 actor_id VARCHAR(64) NOT NULL, action_name VARCHAR(30) NOT NULL, reason VARCHAR(2000) NOT NULL,
 content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_theme_audit (tenant_id, theme_id)
);
CREATE TABLE teaching_plan (
 id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, branch_id VARCHAR(64) NOT NULL,
 content_json LONGTEXT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 INDEX idx_plan_branch (tenant_id, branch_id)
);
