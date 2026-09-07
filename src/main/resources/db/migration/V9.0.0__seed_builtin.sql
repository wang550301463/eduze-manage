-- Builtin permissions
INSERT INTO t_permission (id, code, name, module) VALUES
(1, 'branch:read', '校区查看', 'branch'),
(2, 'branch:write', '校区管理', 'branch'),
(3, 'user:read', '用户查看', 'user'),
(4, 'user:write', '用户管理', 'user'),
(5, 'role:read', '角色查看', 'role'),
(6, 'role:write', '角色管理', 'role'),
(7, 'role:assign', '角色分配', 'role'),
(8, 'student:read', '学员查看', 'student'),
(9, 'student:write', '学员编辑', 'student'),
(10, 'student:import', '学员导入', 'student'),
(11, 'student:delete', '学员删除', 'student'),
(12, 'guardian:read', '家长查看', 'guardian'),
(13, 'guardian:write', '家长编辑', 'guardian'),
(14, 'coursepkg:read', '课时包查看', 'coursepkg'),
(15, 'coursepkg:write', '课时包编辑', 'coursepkg'),
(16, 'course:read', '课程查看', 'course'),
(17, 'course:write', '课程编辑', 'course'),
(18, 'classgroup:read', '班级查看', 'classgroup'),
(19, 'classgroup:write', '班级编辑', 'classgroup'),
(20, 'classgroup:assign', '班级分配', 'classgroup'),
(21, 'classroom:read', '画室查看', 'classroom'),
(22, 'classroom:write', '画室编辑', 'classroom'),
(23, 'lesson:read', '课次查看', 'lesson'),
(24, 'lesson:write', '课次编辑', 'lesson'),
(25, 'lesson:reschedule', '调课', 'lesson'),
(26, 'lesson:cancel', '取消课次', 'lesson'),
(27, 'attendance:read', '出勤查看', 'attendance'),
(28, 'attendance:write', '出勤编辑', 'attendance'),
(29, 'attendance:scan', '扫码签到', 'attendance'),
(30, 'pickup:read', '接送查看', 'pickup'),
(31, 'pickup:write', '接送编辑', 'pickup'),
(32, 'leave:read', '请假查看', 'leave'),
(33, 'leave:write', '请假提交', 'leave'),
(34, 'leave:approve', '请假审批', 'leave'),
(35, 'stat:read', '统计查看', 'stat'),
(36, 'audit:read', '审计查看', 'audit'),
(37, 'search:read', '全局搜索', 'search')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Builtin roles
INSERT INTO t_role (id, tenant_id, code, name, is_builtin) VALUES
(1, 1, 'SUPER_ADMIN', '超级管理员', 1),
(2, 1, 'PRINCIPAL', '校长', 1),
(3, 1, 'ADVISOR', '课程顾问', 1),
(4, 1, 'TEACHER', '任课老师', 1),
(5, 1, 'FRONT_DESK', '前台', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- SUPER_ADMIN: all permissions
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 1000 + p.id, 1, p.id FROM t_permission p;

-- PRINCIPAL: all except user:write, role:*
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 2000 + p.id, 2, p.id FROM t_permission p
WHERE p.code NOT IN ('user:write', 'role:write', 'role:assign');

-- ADVISOR
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 3000 + p.id, 3, p.id FROM t_permission p
WHERE p.code IN (
    'student:read', 'student:write', 'student:import',
    'guardian:read', 'guardian:write',
    'coursepkg:read', 'coursepkg:write',
    'search:read', 'branch:read'
);

-- TEACHER
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 4000 + p.id, 4, p.id FROM t_permission p
WHERE p.code IN (
    'lesson:read', 'lesson:reschedule', 'attendance:read', 'attendance:write',
    'leave:read', 'leave:approve', 'student:read', 'search:read', 'branch:read'
);

-- FRONT_DESK
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 5000 + p.id, 5, p.id FROM t_permission p
WHERE p.code IN (
    'student:read', 'attendance:read', 'attendance:write', 'attendance:scan',
    'pickup:read', 'pickup:write', 'leave:read', 'leave:write', 'search:read', 'branch:read'
);

-- Default admin (password: admin@123)
INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status)
VALUES (
    1001, 1, 1, 'admin',
    '$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW',
    '系统管理员', 1
)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT IGNORE INTO t_user_role (id, tenant_id, user_id, role_id) VALUES (1, 1, 1001, 1);
INSERT IGNORE INTO t_user_branch (id, tenant_id, user_id, branch_id) VALUES (1, 1, 1001, 1);
