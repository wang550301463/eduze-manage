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
