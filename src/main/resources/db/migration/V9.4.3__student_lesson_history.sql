-- ============================================================================
-- V9.4.3 学员历史课程快照（审计用，只追加）
-- ============================================================================

CREATE TABLE IF NOT EXISTS t_student_lesson_history (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(64) NOT NULL,
    lesson_id BIGINT NOT NULL,
    attendance_id BIGINT NULL,
    course_id BIGINT NULL,
    course_name VARCHAR(128) NULL,
    class_group_id BIGINT NULL,
    class_group_name VARCHAR(128) NULL,
    teacher_id BIGINT NULL,
    teacher_name VARCHAR(64) NULL,
    class_room_id BIGINT NULL,
    class_room_name VARCHAR(128) NULL,
    start_at DATETIME(3) NULL,
    end_at DATETIME(3) NULL,
    source INT NULL COMMENT '课次来源 1模板 2手动 3补课 4试听 5考级 6比赛',
    attendance_status INT NULL,
    minutes INT NULL,
    snapshot_json TEXT NULL,
    occurred_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_history_student_lesson (tenant_id, student_id, lesson_id, deleted_at),
    KEY idx_history_occurred (tenant_id, occurred_at),
    KEY idx_history_student_occurred (tenant_id, student_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
