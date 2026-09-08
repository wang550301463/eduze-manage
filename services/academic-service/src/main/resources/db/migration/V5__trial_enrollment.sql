ALTER TABLE academic_trial_reservation
  ADD COLUMN enquiry_id VARCHAR(64) NULL,
  ADD COLUMN enrollment_id VARCHAR(64) NULL,
  ADD COLUMN enrollment_group_id BIGINT NULL,
  ADD UNIQUE KEY uk_trial_enrollment (tenant_id, enrollment_id);
