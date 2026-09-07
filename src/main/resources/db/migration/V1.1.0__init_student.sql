CREATE TABLE IF NOT EXISTS t_student (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    enroll_no VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '0=未知 1=男 2=女',
    birthday DATE NULL,
    enroll_date DATE NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=在读 2=暂停 3=退学',
    allergy VARCHAR(512) NULL,
    health_note VARCHAR(1024) NULL,
    emergency_contact VARCHAR(128) NULL,
    emergency_phone VARCHAR(32) NULL,
    avatar_url VARCHAR(512) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_student_enroll (tenant_id, enroll_no, deleted_at),
    KEY idx_student_branch (tenant_id, branch_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_guardian (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    is_main_contact TINYINT NOT NULL DEFAULT 0,
    can_pickup TINYINT NOT NULL DEFAULT 1,
    qr_code VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_guardian_phone (tenant_id, phone, deleted_at),
    UNIQUE KEY uk_guardian_qr (qr_code, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_student_guardian_relation (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    student_id BIGINT NOT NULL,
    guardian_id BIGINT NOT NULL,
    relation VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_student_guardian (tenant_id, student_id, guardian_id, deleted_at),
    KEY idx_sgr_student (tenant_id, student_id),
    KEY idx_sgr_guardian (tenant_id, guardian_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_course_package (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    total_lessons INT NOT NULL,
    remaining_lessons INT NOT NULL,
    expire_date DATE NULL,
    note VARCHAR(256) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    KEY idx_pkg_student (tenant_id, student_id),
    KEY idx_pkg_branch (tenant_id, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
