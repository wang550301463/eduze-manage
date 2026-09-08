CREATE TABLE family_invite (
 id VARCHAR(36) PRIMARY KEY, tenant_id BIGINT NOT NULL, student_id BIGINT NOT NULL, branch_id BIGINT NOT NULL,
 code_hash CHAR(64) NOT NULL, expires_at TIMESTAMP(3) NOT NULL, claimed_by BIGINT NULL, created_by BIGINT NOT NULL,
 created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), UNIQUE KEY uk_invite_hash(code_hash), KEY idx_invite_student(tenant_id,student_id)
);
CREATE TABLE family_binding (
 id VARCHAR(36) PRIMARY KEY, tenant_id BIGINT NOT NULL, student_id BIGINT NOT NULL, branch_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL, status VARCHAR(16) NOT NULL DEFAULT 'PENDING', invite_id VARCHAR(36) NOT NULL,
 approved_by BIGINT NULL, approved_at TIMESTAMP(3) NULL, revoked_at TIMESTAMP(3) NULL,
 created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE KEY uk_binding_family(tenant_id,student_id,user_id), UNIQUE KEY uk_binding_invite(invite_id), KEY idx_family_user(tenant_id,user_id,status)
);
ALTER TABLE t_leave_request ADD COLUMN family_request_key VARCHAR(64) NULL, ADD UNIQUE KEY uk_leave_family(tenant_id,student_id,family_request_key);
