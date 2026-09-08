CREATE TABLE wechat_identity (
 tenant_id BIGINT NOT NULL, app_id VARCHAR(64) NOT NULL, open_id VARCHAR(128) NOT NULL,
 user_id BIGINT NOT NULL, created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(tenant_id,app_id,open_id), UNIQUE KEY uk_wechat_user(tenant_id,app_id,user_id)
);
-- Front desk can manage approved family relations, but cannot grant employee roles.
INSERT IGNORE INTO t_role_permission(id,role_id,permission_id)
SELECT 8000+p.id,5,p.id FROM t_permission p WHERE p.code IN ('guardian:read','guardian:write');
