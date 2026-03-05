USE student_eval;

SET @has_major_name := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'major_name'
);
SET @sql := IF(
  @has_major_name = 1,
  'ALTER TABLE sys_user DROP COLUMN major_name',
  'SELECT ''[skip] sys_user.major_name already removed'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_notice_audience := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'notice'
    AND column_name = 'audience_scope'
);
SET @sql := IF(
  @has_notice_audience = 0,
  'ALTER TABLE notice ADD COLUMN audience_scope VARCHAR(32) NOT NULL DEFAULT ''ALL_COLLEGE'' AFTER status',
  'SELECT ''[skip] notice.audience_scope exists'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE notice
SET audience_scope = 'ALL_COLLEGE'
WHERE audience_scope IS NULL OR TRIM(audience_scope) = '';

CREATE TABLE IF NOT EXISTS counselor_class_scope (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  counselor_id BIGINT NOT NULL,
  class_name VARCHAR(64) NOT NULL,
  assigned_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_scope_counselor FOREIGN KEY (counselor_id) REFERENCES sys_user(id),
  CONSTRAINT fk_scope_assigned_by FOREIGN KEY (assigned_by) REFERENCES sys_user(id),
  UNIQUE KEY uk_scope_class_name (class_name),
  INDEX idx_scope_counselor_id (counselor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_scope_unique := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'counselor_class_scope'
    AND index_name = 'uk_scope_class_name'
);
SET @sql := IF(
  @has_scope_unique = 0,
  'ALTER TABLE counselor_class_scope ADD UNIQUE KEY uk_scope_class_name (class_name)',
  'SELECT ''[skip] uk_scope_class_name exists'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_scope_idx := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'counselor_class_scope'
    AND index_name = 'idx_scope_counselor_id'
);
SET @sql := IF(
  @has_scope_idx = 0,
  'ALTER TABLE counselor_class_scope ADD INDEX idx_scope_counselor_id (counselor_id)',
  'SELECT ''[skip] idx_scope_counselor_id exists'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS notice_target_class (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notice_id BIGINT NOT NULL,
  class_name VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notice_target_notice FOREIGN KEY (notice_id) REFERENCES notice(id),
  UNIQUE KEY uk_notice_class (notice_id, class_name),
  INDEX idx_notice_target_class_name (class_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_notice_class_unique := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'notice_target_class'
    AND index_name = 'uk_notice_class'
);
SET @sql := IF(
  @has_notice_class_unique = 0,
  'ALTER TABLE notice_target_class ADD UNIQUE KEY uk_notice_class (notice_id, class_name)',
  'SELECT ''[skip] uk_notice_class exists'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_notice_class_idx := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'notice_target_class'
    AND index_name = 'idx_notice_target_class_name'
);
SET @sql := IF(
  @has_notice_class_idx = 0,
  'ALTER TABLE notice_target_class ADD INDEX idx_notice_target_class_name (class_name)',
  'SELECT ''[skip] idx_notice_target_class_name exists'' AS migration_info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'migration_20260306_counselor_scope_and_notice_audience_done' AS result;
