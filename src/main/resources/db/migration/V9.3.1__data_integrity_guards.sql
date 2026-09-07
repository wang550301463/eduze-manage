-- V9.3.1: fail-fast data integrity guards before production cutover.
-- Does not rewrite historical migrations; only asserts current data quality.
-- Unique enroll_no already enforced by uk_student_enroll (tenant_id, enroll_no, deleted_at).

-- Orphan students: branch must exist
SET @orphan_student_branch := (
  SELECT COUNT(*) FROM t_student s
  LEFT JOIN t_branch b ON b.id = s.branch_id AND b.deleted_at = 0
  WHERE s.deleted_at = 0 AND b.id IS NULL
);
SET @msg := IF(@orphan_student_branch > 0,
  CONCAT('V9.3.1 blocked: orphan student.branch_id count=', @orphan_student_branch),
  'ok');
SET @sql := IF(@orphan_student_branch > 0, CONCAT('SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''', @msg, ''''), 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Duplicate active enroll_no within tenant
SET @dup_enroll := (
  SELECT COUNT(*) FROM (
    SELECT tenant_id, enroll_no, COUNT(*) c
    FROM t_student
    WHERE deleted_at = 0 AND enroll_no IS NOT NULL AND enroll_no <> ''
    GROUP BY tenant_id, enroll_no
    HAVING c > 1
  ) d
);
SET @msg2 := IF(@dup_enroll > 0,
  CONCAT('V9.3.1 blocked: duplicate enroll_no count=', @dup_enroll),
  'ok');
SET @sql2 := IF(@dup_enroll > 0, CONCAT('SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''', @msg2, ''''), 'SELECT 1');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- Marker row for operators (no-op success)
SELECT 'V9.3.1 data integrity guards passed' AS status;
