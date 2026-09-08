ALTER TABLE t_student_lesson_hour_ledger
 ADD COLUMN remaining_lessons_after INT NULL,
 ADD UNIQUE KEY uk_ledger_attend(tenant_id,student_id,lesson_id,event_type),
 ADD UNIQUE KEY uk_ledger_reversal(tenant_id,related_ledger_id,event_type);
ALTER TABLE t_course_package ADD COLUMN course_id BIGINT NULL,
 ADD KEY idx_package_eligible(tenant_id,student_id,branch_id,course_id,expire_date);
