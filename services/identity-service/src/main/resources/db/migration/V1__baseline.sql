CREATE TABLE IF NOT EXISTS t_tenant (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_tenant_code (code, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_branch (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL,
    address VARCHAR(256) NULL,
    phone VARCHAR(32) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_branch_code (tenant_id, code, deleted_at),
    KEY idx_branch_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO t_tenant (id, name, code, status)
VALUES (1, '默认机构', 'DEFAULT', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO t_branch (id, tenant_id, name, code, address, status)
VALUES (1, 1, '总校区', 'HQ', '', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    name VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    last_login_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_user_username (tenant_id, username, deleted_at),
    KEY idx_user_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_role (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    is_builtin TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_role_code (tenant_id, code, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_permission (
    id BIGINT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    module VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_user_role (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_user_role (tenant_id, user_id, role_id),
    KEY idx_user_role_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_role_permission (
    id BIGINT NOT NULL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_user_branch (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_user_branch (tenant_id, user_id, branch_id),
    KEY idx_user_branch_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_audit_log (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    branch_id BIGINT NULL,
    user_id BIGINT NULL,
    username VARCHAR(64) NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NULL,
    entity_id VARCHAR(64) NULL,
    ip VARCHAR(64) NULL,
    user_agent VARCHAR(256) NULL,
    request_path VARCHAR(512) NULL,
    status VARCHAR(16) NOT NULL,
    error_msg VARCHAR(512) NULL,
    extra_json JSON NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_tenant_created (tenant_id, created_at),
    KEY idx_audit_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

-- 课程顾问不应查看/管理校区列表（与 BranchControllerIT、投产 RBAC 一致）
DELETE FROM t_role_permission WHERE role_id = 3 AND permission_id = 1;

-- Session invalidation support: bump token_version to revoke outstanding JWTs.
ALTER TABLE t_user
    ADD COLUMN token_version INT NOT NULL DEFAULT 1 COMMENT 'JWT session version' AFTER last_login_at;

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
