ALTER TABLE t_course_package ADD COLUMN source_order_id VARCHAR(64) NULL,
 ADD COLUMN frozen_lessons INT NOT NULL DEFAULT 0,
 ADD UNIQUE KEY uk_package_order(tenant_id,source_order_id),
 ADD CONSTRAINT ck_package_frozen CHECK(frozen_lessons>=0 AND remaining_lessons>=frozen_lessons);
CREATE TABLE academic_trial_reservation(
 reservation_id VARCHAR(64) NOT NULL,tenant_id BIGINT NOT NULL,branch_id BIGINT NOT NULL,
 session_id BIGINT NOT NULL,student_id BIGINT NOT NULL,request_hash CHAR(64) NOT NULL,
 status VARCHAR(16) NOT NULL,created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(tenant_id,reservation_id),KEY idx_trial_session(tenant_id,session_id,status)
);
CREATE TABLE academic_refund(
 refund_id VARCHAR(64) NOT NULL,tenant_id BIGINT NOT NULL,branch_id BIGINT NOT NULL,
 order_id VARCHAR(64) NOT NULL,student_id BIGINT NOT NULL,package_id BIGINT NOT NULL,
 lesson_units INT NOT NULL,status VARCHAR(16) NOT NULL,created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(tenant_id,refund_id),KEY idx_refund_order(tenant_id,order_id,status)
);
ALTER TABLE t_student_lesson_hour_ledger ADD COLUMN lesson_units_delta INT NULL,
 ADD COLUMN external_reference VARCHAR(96) NULL,
 ADD UNIQUE KEY uk_ledger_external(tenant_id,external_reference);
