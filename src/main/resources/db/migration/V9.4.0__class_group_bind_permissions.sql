-- ============================================================================
-- V9.4.0 分组绑定 + 课时调账权限
-- ============================================================================

INSERT INTO t_permission (id, code, name, module) VALUES
 (44, 'student:hour_adjust', '学员课时调账', 'student')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 1000 + p.id, 1, p.id FROM t_permission p WHERE p.code = 'student:hour_adjust';

INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 2000 + p.id, 2, p.id FROM t_permission p WHERE p.code = 'student:hour_adjust';
