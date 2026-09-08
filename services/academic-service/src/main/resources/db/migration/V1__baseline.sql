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

CREATE TABLE IF NOT EXISTS t_course (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(128) NOT NULL,
    age_min INT NULL,
    age_max INT NULL,
    lesson_minutes INT NOT NULL DEFAULT 60,
    cover_url VARCHAR(512) NULL,
    description TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_course_name (tenant_id, name, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_class_group (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    course_id BIGINT NOT NULL,
    head_teacher_id BIGINT NULL,
    capacity INT NOT NULL DEFAULT 20,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    KEY idx_class_group_branch (tenant_id, branch_id),
    KEY idx_class_group_name (tenant_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_class_room (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    capacity INT NOT NULL DEFAULT 20,
    note VARCHAR(512) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    KEY idx_class_room_branch (tenant_id, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_student_class_group (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    student_id BIGINT NOT NULL,
    class_group_id BIGINT NOT NULL,
    joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    left_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    KEY idx_scg_student (tenant_id, student_id),
    KEY idx_scg_class (tenant_id, class_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_lesson (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    class_group_id BIGINT NOT NULL,
    class_room_id BIGINT NULL,
    teacher_id BIGINT NULL,
    start_at DATETIME(3) NOT NULL,
    end_at DATETIME(3) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=待开课 2=进行中 3=已完成 4=已取消',
    note VARCHAR(512) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    KEY idx_lesson_branch_start (tenant_id, branch_id, start_at),
    KEY idx_lesson_class_start (class_group_id, start_at),
    KEY idx_lesson_teacher_start (teacher_id, start_at),
    KEY idx_lesson_room_start (class_room_id, start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_lesson_change_log (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    lesson_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    before_json JSON NULL,
    after_json JSON NULL,
    reason VARCHAR(256) NULL,
    operator_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    KEY idx_lesson_change_lesson (tenant_id, lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_attendance (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 2 COMMENT '2=已入园 3=已离园 4=缺勤 5=请假',
    check_in_at DATETIME(3) NULL,
    check_out_at DATETIME(3) NULL,
    check_in_method VARCHAR(16) NULL COMMENT 'manual/qr/auto/face_reserved',
    note VARCHAR(256) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_attendance_lesson_student (tenant_id, lesson_id, student_id, deleted_at),
    KEY idx_attendance_branch_lesson (tenant_id, branch_id, lesson_id),
    KEY idx_attendance_student_lesson (student_id, lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_pickup_record (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    attendance_id BIGINT NOT NULL,
    event_type VARCHAR(8) NOT NULL COMMENT 'in/out',
    guardian_id BIGINT NULL,
    is_abnormal TINYINT NOT NULL DEFAULT 0,
    abnormal_note VARCHAR(256) NULL,
    event_time DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    KEY idx_pickup_attendance (attendance_id),
    KEY idx_pickup_branch_time (tenant_id, branch_id, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_leave_request (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    lesson_id BIGINT NULL,
    leave_start_date DATE NOT NULL,
    leave_end_date DATE NOT NULL,
    reason VARCHAR(256) NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=待审 2=已批 3=已拒',
    approved_by BIGINT NULL,
    approved_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    KEY idx_leave_student (student_id, status),
    KEY idx_leave_status_dates (status, leave_start_date),
    KEY idx_leave_branch (tenant_id, branch_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- V1.5.0 老师为核心的核心表 DDL
-- - 学员强制关联主带老师 (mentor_teacher_id)
-- - 老师维护周可用时段模板 (t_teacher_availability)
-- - 学员订阅 (t_lesson_subscription) + 课次名单 (t_lesson_student)
-- - 主带老师变更历史 (t_student_mentor_history)
-- - lesson 表加 source / teacher_availability_id
-- - class_group / lesson 的 course_id / class_group_id 改可空（班级降级）
-- ============================================================================

-- 1. 学员表加字段
ALTER TABLE t_student
    ADD COLUMN mentor_teacher_id BIGINT NULL COMMENT '主带老师 user.id'
        AFTER avatar_url,
    ADD COLUMN current_stage_id BIGINT NULL COMMENT '当前阶段 curriculum_stage.id'
        AFTER mentor_teacher_id,
    ADD KEY idx_student_mentor (tenant_id, mentor_teacher_id),
    ADD KEY idx_student_stage  (tenant_id, current_stage_id);

-- 2. 老师可用时段（周课表模板）
CREATE TABLE IF NOT EXISTS t_teacher_availability (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    day_of_week TINYINT NOT NULL COMMENT '1=周一 ... 7=周日',
    start_minute SMALLINT NOT NULL COMMENT '0-1439',
    end_minute   SMALLINT NOT NULL,
    capacity INT NOT NULL DEFAULT 8,
    default_class_room_id BIGINT NULL,
    valid_from DATE NOT NULL,
    valid_to   DATE NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    note VARCHAR(256) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_ta_slot (tenant_id, teacher_id, day_of_week, start_minute, valid_from, deleted_at),
    KEY idx_ta_branch_teacher (tenant_id, branch_id, teacher_id, day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 学员订阅
CREATE TABLE IF NOT EXISTS t_lesson_subscription (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    teacher_availability_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to   DATE NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    source VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/MAKEUP',
    note VARCHAR(256) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_sub_slot (tenant_id, student_id, teacher_availability_id, valid_from, deleted_at),
    KEY idx_sub_availability_status (tenant_id, teacher_availability_id, status),
    KEY idx_sub_student_status (tenant_id, student_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 课次名单
CREATE TABLE IF NOT EXISTS t_lesson_student (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    subscription_id BIGINT NULL,
    source VARCHAR(16) NOT NULL DEFAULT 'SUBSCRIPTION' COMMENT 'SUBSCRIPTION/MANUAL/TRIAL/MAKEUP',
    status VARCHAR(16) NOT NULL DEFAULT 'BOOKED' COMMENT 'BOOKED/CANCELLED',
    note VARCHAR(256) NULL,
    removed_at DATETIME(3) NULL,
    removed_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_ls_lesson_student (tenant_id, lesson_id, student_id, deleted_at),
    KEY idx_ls_lesson (tenant_id, lesson_id),
    KEY idx_ls_student (tenant_id, student_id),
    KEY idx_ls_subscription (tenant_id, subscription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 主带老师变更历史
CREATE TABLE IF NOT EXISTS t_student_mentor_history (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    from_teacher_id BIGINT NULL COMMENT 'NULL=首次绑定',
    to_teacher_id BIGINT NOT NULL,
    reason VARCHAR(256) NULL,
    changed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    operator_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_smh_student (tenant_id, student_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. lesson 表加字段 + 弱化 class_group_id 约束 + 把 status 改为 VARCHAR
-- 历史 TINYINT 值映射：1→SCHEDULED, 2→IN_PROGRESS, 3→COMPLETED, 4→CANCELLED
ALTER TABLE t_lesson
    ADD COLUMN teacher_availability_id BIGINT NULL COMMENT '生成源',
    ADD COLUMN source TINYINT NOT NULL DEFAULT 2 COMMENT '1=模板生成 2=手动 3=补课 4=试听',
    MODIFY COLUMN class_group_id BIGINT NULL,
    MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED';

-- 历史数值 status 转换为枚举字符串（如果有任何旧数据）
UPDATE t_lesson SET status = 'SCHEDULED'   WHERE status = '1';
UPDATE t_lesson SET status = 'IN_PROGRESS' WHERE status = '2';
UPDATE t_lesson SET status = 'COMPLETED'   WHERE status = '3';
UPDATE t_lesson SET status = 'CANCELLED'   WHERE status = '4';

-- 7. class_group 字段调整
ALTER TABLE t_class_group
    MODIFY COLUMN course_id BIGINT NULL,
    ADD COLUMN tag_color CHAR(7) NULL COMMENT '#RRGGBB';

-- ============================================================================
-- V1.6.0 分组标签绑定老师可用时段
-- ============================================================================

ALTER TABLE t_class_group
    ADD COLUMN teacher_availability_id BIGINT NULL COMMENT '绑定的老师可用时段' AFTER head_teacher_id;

CREATE UNIQUE INDEX uk_class_group_availability
    ON t_class_group (tenant_id, teacher_availability_id, deleted_at);

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
