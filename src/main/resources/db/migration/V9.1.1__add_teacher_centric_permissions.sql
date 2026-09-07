-- ============================================================================
-- V9.1.1 老师为核心的权限补丁
-- 6 项新权限 + 4 个角色（SUPER_ADMIN/PRINCIPAL/ADVISOR/TEACHER）补丁
-- ============================================================================

INSERT INTO t_permission (id, code, name, module) VALUES
 (38, 'teacher:availability:read',  '老师可用时段查看', 'teacher'),
 (39, 'teacher:availability:write', '老师可用时段编辑', 'teacher'),
 (40, 'subscription:read',          '课程订阅查看',     'subscription'),
 (41, 'subscription:write',         '课程订阅编辑',     'subscription'),
 (42, 'student:mentor_assign',      '主带老师指派',     'student'),
 (43, 'lesson:teacher_view',        '按老师查看课表',   'lesson')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- TEACHER (id=4) ← +avail:write/sub:read/lesson:teacher_view
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 4000 + p.id, 4, p.id FROM t_permission p
WHERE p.code IN ('teacher:availability:read','teacher:availability:write','subscription:read','lesson:teacher_view');

-- ADVISOR (id=3) ← +sub:write/mentor_assign/lesson:teacher_view
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 3000 + p.id, 3, p.id FROM t_permission p
WHERE p.code IN ('subscription:read','subscription:write','student:mentor_assign','lesson:teacher_view');

-- PRINCIPAL (id=2) ← 上述全部
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 2000 + p.id, 2, p.id FROM t_permission p
WHERE p.code IN ('teacher:availability:read','teacher:availability:write',
                 'subscription:read','subscription:write',
                 'student:mentor_assign','lesson:teacher_view');

-- SUPER_ADMIN (id=1) ← 上述全部
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 1000 + p.id, 1, p.id FROM t_permission p
WHERE p.code IN ('teacher:availability:read','teacher:availability:write',
                 'subscription:read','subscription:write',
                 'student:mentor_assign','lesson:teacher_view');
