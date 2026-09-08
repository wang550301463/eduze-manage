INSERT INTO t_permission(id,code,name,module) VALUES (106,'media:manage','媒体清理复核','media');
INSERT IGNORE INTO t_role_permission(id,role_id,permission_id)
SELECT 500000+r.id*1000+p.id,r.id,p.id FROM t_role r JOIN t_permission p ON p.code='media:manage' WHERE r.code IN ('SUPER_ADMIN','PRINCIPAL');
