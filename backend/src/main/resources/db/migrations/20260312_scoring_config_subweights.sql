USE student_eval;

-- Non-destructive migration: add per-semester sub-weights for Intel/Sport calculation.
SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'scoring_config'
        AND COLUMN_NAME = 'intel_course_ratio'
    ),
    'SELECT 1',
    'ALTER TABLE scoring_config ADD COLUMN intel_course_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.8500 AFTER w_intel'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'scoring_config'
        AND COLUMN_NAME = 'intel_innovation_ratio'
    ),
    'SELECT 1',
    'ALTER TABLE scoring_config ADD COLUMN intel_innovation_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.1500 AFTER intel_course_ratio'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'scoring_config'
        AND COLUMN_NAME = 'sport_university_pe_ratio'
    ),
    'SELECT 1',
    'ALTER TABLE scoring_config ADD COLUMN sport_university_pe_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.8500 AFTER w_sport'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'scoring_config'
        AND COLUMN_NAME = 'sport_activity_ratio'
    ),
    'SELECT 1',
    'ALTER TABLE scoring_config ADD COLUMN sport_activity_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.1500 AFTER sport_university_pe_ratio'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
