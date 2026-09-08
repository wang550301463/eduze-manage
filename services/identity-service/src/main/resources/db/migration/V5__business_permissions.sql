INSERT INTO t_permission(id,code,name,module) VALUES
(104,'engagement:write','招生与活动管理','engagement'),
(105,'commerce:write','商品订单与退款管理','commerce');
INSERT IGNORE INTO t_role_permission(id,role_id,permission_id)
SELECT 300000+r.id*1000+p.id,r.id,p.id FROM t_role r JOIN t_permission p
ON p.code IN ('engagement:write','classgroup:read','classgroup:assign')
WHERE r.code IN ('SUPER_ADMIN','PRINCIPAL','FRONT_DESK','ADVISOR');
INSERT IGNORE INTO t_role_permission(id,role_id,permission_id)
SELECT 400000+r.id*1000+p.id,r.id,p.id FROM t_role r JOIN t_permission p
ON p.code='commerce:write' WHERE r.code IN ('SUPER_ADMIN','PRINCIPAL');
