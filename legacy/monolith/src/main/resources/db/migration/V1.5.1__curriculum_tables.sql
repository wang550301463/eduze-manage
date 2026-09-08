-- ============================================================================
-- V1.5.1 课程体系（罗恩菲尔德 5 阶段 + 778 维度）数据表
-- ============================================================================

-- 1. 阶段
CREATE TABLE IF NOT EXISTS t_curriculum_stage (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    age_min INT NOT NULL,
    age_max INT NOT NULL,
    order_no INT NOT NULL DEFAULT 0,
    lorenfield_phase VARCHAR(64) NULL,
    description VARCHAR(512) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_stage_code (tenant_id, code, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 维度
CREATE TABLE IF NOT EXISTS t_curriculum_dimension (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    kind VARCHAR(16) NOT NULL COMMENT 'ELEMENT/PRINCIPLE/MOVEMENT',
    code VARCHAR(48) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(512) NULL,
    order_no INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_dim_code (tenant_id, code, deleted_at),
    KEY idx_dim_kind (kind, order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 阶段 ↔ 维度
CREATE TABLE IF NOT EXISTS t_stage_dimension (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    stage_id BIGINT NOT NULL,
    dimension_id BIGINT NOT NULL,
    weight TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_stage_dim (tenant_id, stage_id, dimension_id),
    KEY idx_sd_stage (stage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 学员阶段评估
CREATE TABLE IF NOT EXISTS t_student_stage_assessment (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    stage_id BIGINT NOT NULL,
    assessed_at DATE NOT NULL,
    assessed_by BIGINT NULL,
    scores_json JSON NULL COMMENT '{dimension_code: 1-5}',
    comment VARCHAR(1024) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    KEY idx_ssa_student (tenant_id, student_id, assessed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
