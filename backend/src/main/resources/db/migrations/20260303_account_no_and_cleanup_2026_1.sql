USE student_eval;

-- One-time migration:
-- 1) clean sys_user rows where class_name = '2026-1' (must be exactly 12)
-- 2) drop legacy username column
-- 3) rename student_no -> account_no
-- 4) enforce unique key on account_no

SET @expected_user_count := 12;
SET @actual_user_count := (
  SELECT COUNT(*) FROM sys_user WHERE class_name = '2026-1'
);
SET @non_student_count := (
  SELECT COUNT(*) FROM sys_user WHERE class_name = '2026-1' AND role <> 'STUDENT'
);

-- Guard: user count must match expectation.
SET @sql := IF(
  @actual_user_count = @expected_user_count,
  'SELECT ''[ok] cleanup target count matched (12)'' AS migration_info',
  CONCAT(
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Cleanup aborted: expected 12 users in class_name=2026-1, found ',
    @actual_user_count,
    ''''
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Guard: all target users must be students.
SET @sql := IF(
  @non_student_count = 0,
  'SELECT ''[ok] cleanup role guard matched (all STUDENT)'' AS migration_info',
  CONCAT(
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Cleanup aborted: class_name=2026-1 contains non-STUDENT users (',
    @non_student_count,
    ')'''
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Guard: target users must not have related business rows.
SET @submission_ref_count := (
  SELECT COUNT(*) FROM submission s
  WHERE s.student_id IN (SELECT id FROM sys_user WHERE class_name = '2026-1')
);
SET @course_ref_count := (
  SELECT COUNT(*)
  FROM course_item ci
  JOIN submission s ON s.id = ci.submission_id
  WHERE s.student_id IN (SELECT id FROM sys_user WHERE class_name = '2026-1')
);
SET @activity_ref_count := (
  SELECT COUNT(*)
  FROM activity_item ai
  JOIN submission s ON s.id = ai.submission_id
  WHERE s.student_id IN (SELECT id FROM sys_user WHERE class_name = '2026-1')
);
SET @review_log_ref_count := (
  SELECT COUNT(*)
  FROM review_log rl
  JOIN submission s ON s.id = rl.submission_id
  WHERE s.student_id IN (SELECT id FROM sys_user WHERE class_name = '2026-1')
);
SET @attachment_ref_count := (
  SELECT COUNT(*) FROM attachment a
  WHERE a.uploader_id IN (SELECT id FROM sys_user WHERE class_name = '2026-1')
);
SET @feedback_creator_ref_count := (
  SELECT COUNT(*) FROM feedback f
  WHERE f.creator_id IN (SELECT id FROM sys_user WHERE class_name = '2026-1')
);
SET @feedback_handler_ref_count := (
  SELECT COUNT(*) FROM feedback f
  WHERE f.handler_id IN (SELECT id FROM sys_user WHERE class_name = '2026-1')
);
SET @notice_ref_count := (
  SELECT COUNT(*) FROM notice n
  WHERE n.publisher_id IN (SELECT id FROM sys_user WHERE class_name = '2026-1')
);

SET @related_total :=
  @submission_ref_count +
  @course_ref_count +
  @activity_ref_count +
  @review_log_ref_count +
  @attachment_ref_count +
  @feedback_creator_ref_count +
  @feedback_handler_ref_count +
  @notice_ref_count;

SET @sql := IF(
  @related_total = 0,
  'SELECT ''[ok] no related business data for cleanup users'' AS migration_info',
  CONCAT(
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Cleanup aborted: related business rows exist (',
    @related_total,
    ')'''
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Print target list for audit logs.
SELECT
  id,
  real_name,
  role,
  class_name
FROM sys_user
WHERE class_name = '2026-1'
ORDER BY id;

-- Create backup table once; schema copied from current sys_user.
CREATE TABLE IF NOT EXISTS sys_user_deleted_20260303 AS
SELECT u.*, CAST(NULL AS DATETIME) AS deleted_at
FROM sys_user u
WHERE 1 = 0;

START TRANSACTION;

SET @actual_user_count_tx := (
  SELECT COUNT(*) FROM sys_user WHERE class_name = '2026-1'
);
SET @sql := IF(
  @actual_user_count_tx = @expected_user_count,
  'SELECT ''[ok] transaction pre-check matched'' AS migration_info',
  CONCAT(
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Cleanup aborted in transaction: expected 12 users, found ',
    @actual_user_count_tx,
    ''''
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_user_deleted_20260303
SELECT u.*, NOW() AS deleted_at
FROM sys_user u
WHERE u.class_name = '2026-1';

DELETE FROM sys_user
WHERE class_name = '2026-1';

SET @remaining_user_count := (
  SELECT COUNT(*) FROM sys_user WHERE class_name = '2026-1'
);
SET @sql := IF(
  @remaining_user_count = 0,
  'SELECT ''[ok] cleanup delete completed'' AS migration_info',
  CONCAT(
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Cleanup failed: remaining users in class_name=2026-1 = ',
    @remaining_user_count,
    ''''
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

COMMIT;

SELECT COUNT(*) AS cleanup_remaining
FROM sys_user
WHERE class_name = '2026-1';

-- Schema cleanup: student_no -> account_no, remove username.
SET @has_student_no := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'student_no'
);
SET @has_account_no := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'account_no'
);

SET @sql := IF(
  @has_student_no = 1 AND @has_account_no = 1,
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Schema conflict: both student_no and account_no exist in sys_user''',
  'SELECT ''[ok] no dual-column conflict in sys_user'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @has_student_no = 1 AND @has_account_no = 0,
  'ALTER TABLE sys_user CHANGE COLUMN student_no account_no VARCHAR(32) DEFAULT NULL',
  'SELECT ''[skip] no student_no rename needed'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_username := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'username'
);
SET @sql := IF(
  @has_username = 1,
  'ALTER TABLE sys_user DROP COLUMN username',
  'SELECT ''[skip] username already removed'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_old_unique := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND index_name = 'uk_sys_user_student_no'
);
SET @sql := IF(
  @has_old_unique > 0,
  'ALTER TABLE sys_user DROP INDEX uk_sys_user_student_no',
  'SELECT ''[skip] old uk_sys_user_student_no not found'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_account_no_after := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'account_no'
);
SET @sql := IF(
  @has_account_no_after = 1,
  'SELECT ''[ok] account_no column is present'' AS migration_info',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Schema cleanup failed: account_no column missing in sys_user'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_account_no_unique := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND index_name = 'uk_sys_user_account_no'
);
SET @sql := IF(
  @has_account_no_unique = 0,
  'ALTER TABLE sys_user ADD UNIQUE KEY uk_sys_user_account_no (account_no)',
  'SELECT ''[skip] uk_sys_user_account_no already exists'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'migration_20260303_account_no_and_cleanup_2026_1 completed' AS result;
