INSERT INTO t_permission(id,code,name,module) VALUES
 (101,'teaching:write','教学主题与课件编辑','teaching'),
 (102,'portfolio:write','作品课效编辑发布','portfolio'),
 (103,'exhibition:approve','公开作品展览审核','portfolio');
INSERT IGNORE INTO t_role_permission(id,role_id,permission_id)
SELECT 100000+r.id*1000+p.id,r.id,p.id FROM t_role r JOIN t_permission p
 ON p.code IN ('teaching:write','portfolio:write','course:read')
WHERE r.code IN ('TEACHER','PRINCIPAL','SUPER_ADMIN');
INSERT IGNORE INTO t_role_permission(id,role_id,permission_id)
SELECT 200000+r.id*1000+p.id,r.id,p.id FROM t_role r JOIN t_permission p ON p.code='exhibition:approve'
WHERE r.code IN ('PRINCIPAL','SUPER_ADMIN');
