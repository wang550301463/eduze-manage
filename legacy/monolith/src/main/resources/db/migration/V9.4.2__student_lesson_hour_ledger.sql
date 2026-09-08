-- ============================================================================
-- V1.6.1 学员历史课时审计流水（只追加）
-- ============================================================================

CREATE TABLE IF NOT EXISTS t_student_lesson_hour_ledger (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    lesson_id BIGINT NULL,
    lesson_student_id BIGINT NULL,
    package_id BIGINT NULL,
    event_type VARCHAR(32) NOT NULL COMMENT 'ATTEND/ABSENT_DEDUCT/MAKEUP/ADJUST/VOID',
    minutes_delta INT NOT NULL,
    balance_after_minutes INT NULL,
    occurred_at DATETIME(3) NOT NULL,
    operator_id BIGINT NULL,
    note VARCHAR(512) NULL,
    related_ledger_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    KEY idx_ledger_student (tenant_id, student_id, occurred_at),
    KEY idx_ledger_lesson (tenant_id, lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
