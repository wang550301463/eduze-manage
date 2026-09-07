-- ============================================================================
-- V1.6.0 分组标签绑定老师可用时段
-- ============================================================================

ALTER TABLE t_class_group
    ADD COLUMN teacher_availability_id BIGINT NULL COMMENT '绑定的老师可用时段' AFTER head_teacher_id;

CREATE UNIQUE INDEX uk_class_group_availability
    ON t_class_group (tenant_id, teacher_availability_id, deleted_at);
