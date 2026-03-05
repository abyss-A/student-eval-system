USE student_eval;

SET @has_course_delete_state := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'course_item'
    AND column_name = 'delete_state'
);
SET @sql := IF(
  @has_course_delete_state = 0,
  'ALTER TABLE course_item ADD COLUMN delete_state VARCHAR(32) NOT NULL DEFAULT ''NONE'' AFTER review_status',
  'SELECT ''[skip] course_item.delete_state exists'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_activity_delete_state := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'activity_item'
    AND column_name = 'delete_state'
);
SET @sql := IF(
  @has_activity_delete_state = 0,
  'ALTER TABLE activity_item ADD COLUMN delete_state VARCHAR(32) NOT NULL DEFAULT ''NONE'' AFTER review_status',
  'SELECT ''[skip] activity_item.delete_state exists'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE course_item
SET delete_state = 'NONE'
WHERE delete_state IS NULL OR TRIM(delete_state) = '';

UPDATE activity_item
SET delete_state = 'NONE'
WHERE delete_state IS NULL OR TRIM(delete_state) = '';

SET @has_course_idx := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'course_item'
    AND index_name = 'idx_course_submission_delete_state'
);
SET @sql := IF(
  @has_course_idx = 0,
  'CREATE INDEX idx_course_submission_delete_state ON course_item(submission_id, delete_state)',
  'SELECT ''[skip] idx_course_submission_delete_state exists'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_activity_idx := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'activity_item'
    AND index_name = 'idx_activity_submission_delete_state'
);
SET @sql := IF(
  @has_activity_idx = 0,
  'CREATE INDEX idx_activity_submission_delete_state ON activity_item(submission_id, delete_state)',
  'SELECT ''[skip] idx_activity_submission_delete_state exists'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'migration_delete_state_review_v2_done' AS result;
