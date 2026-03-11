CREATE DATABASE IF NOT EXISTS student_eval DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE student_eval;

DROP TABLE IF EXISTS review_log;
DROP TABLE IF EXISTS activity_item;
DROP TABLE IF EXISTS course_item;
DROP TABLE IF EXISTS attachment;
DROP TABLE IF EXISTS submission;
DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS notice_target_class;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS counselor_class_scope;
DROP TABLE IF EXISTS scoring_config;
DROP TABLE IF EXISTS semester;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  account_no VARCHAR(32) DEFAULT NULL,
  real_name VARCHAR(64) NOT NULL,
  gender VARCHAR(16) DEFAULT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  class_name VARCHAR(64) DEFAULT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_user_account_no (account_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE semester (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  year_num INT NOT NULL,
  term_num INT NOT NULL,
  is_active TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE scoring_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  semester_id BIGINT NOT NULL,
  appeal_days INT NOT NULL DEFAULT 10,
  precedence_mode VARCHAR(32) NOT NULL DEFAULT 'COLLEGE_FIRST',
  score_model VARCHAR(32) NOT NULL DEFAULT 'STRICT_FORMULA',
  w_moral DECIMAL(8,4) NOT NULL DEFAULT 0.1500,
  w_intel DECIMAL(8,4) NOT NULL DEFAULT 0.6000,
  intel_course_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.8500,
  intel_innovation_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.1500,
  w_sport DECIMAL(8,4) NOT NULL DEFAULT 0.1000,
  sport_university_pe_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.8500,
  sport_activity_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.1500,
  w_art DECIMAL(8,4) NOT NULL DEFAULT 0.0750,
  w_labor DECIMAL(8,4) NOT NULL DEFAULT 0.0750,
  cap_moral DECIMAL(8,2) NOT NULL DEFAULT 100.00,
  cap_intel DECIMAL(8,2) NOT NULL DEFAULT 100.00,
  cap_sport DECIMAL(8,2) NOT NULL DEFAULT 100.00,
  cap_art DECIMAL(8,2) NOT NULL DEFAULT 100.00,
  cap_labor DECIMAL(8,2) NOT NULL DEFAULT 100.00,
  CONSTRAINT fk_sc_semester FOREIGN KEY (semester_id) REFERENCES semester(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE submission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  semester_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  moral_raw DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  intel_raw DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  sport_raw DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  art_raw DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  labor_raw DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  total_score DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  submitted_at DATETIME NULL,
  finalized_at DATETIME NULL,
  published_at DATETIME NULL,
  counselor_ready_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_submission_semester_student (semester_id, student_id),
  CONSTRAINT fk_sub_semester FOREIGN KEY (semester_id) REFERENCES semester(id),
  CONSTRAINT fk_sub_student FOREIGN KEY (student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE course_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  course_name VARCHAR(128) NOT NULL,
  course_type VARCHAR(32) NOT NULL,
  score DECIMAL(8,2) NOT NULL,
  credit DECIMAL(8,2) NOT NULL,
  evidence_file_id BIGINT NULL,
  review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  delete_state VARCHAR(32) NOT NULL DEFAULT 'NONE',
  reviewer_score DECIMAL(8,2) NULL,
  reviewer_comment VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_course_submission FOREIGN KEY (submission_id) REFERENCES submission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE activity_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  module_type VARCHAR(32) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(1000) NULL,
  self_score DECIMAL(8,2) NOT NULL,
  final_score DECIMAL(8,2) NULL,
  evidence_file_ids VARCHAR(500) NULL,
  review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  delete_state VARCHAR(32) NOT NULL DEFAULT 'NONE',
  reviewer_comment VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_activity_submission FOREIGN KEY (submission_id) REFERENCES submission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE review_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_type VARCHAR(32) NOT NULL,
  item_id BIGINT NOT NULL,
  submission_id BIGINT NOT NULL,
  action VARCHAR(32) NOT NULL,
  score_before DECIMAL(8,2) NULL,
  score_after DECIMAL(8,2) NULL,
  reason VARCHAR(500) NULL,
  reviewer_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_submission FOREIGN KEY (submission_id) REFERENCES submission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT NULL,
  uploader_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_size BIGINT NOT NULL,
  mime_type VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_attachment_user FOREIGN KEY (uploader_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  audience_scope VARCHAR(32) NOT NULL DEFAULT 'ALL_COLLEGE',
  publisher_id BIGINT NOT NULL,
  published_at DATETIME NULL,
  offline_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id) REFERENCES sys_user(id),
  INDEX idx_notice_status (status),
  INDEX idx_notice_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE counselor_class_scope (
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

CREATE TABLE notice_target_class (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notice_id BIGINT NOT NULL,
  class_name VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notice_target_notice FOREIGN KEY (notice_id) REFERENCES notice(id),
  UNIQUE KEY uk_notice_class (notice_id, class_name),
  INDEX idx_notice_target_class_name (class_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  creator_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  screenshot_file_ids VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'NEW',
  handler_id BIGINT NULL,
  reply_content TEXT NULL,
  replied_at DATETIME NULL,
  closed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_feedback_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id),
  CONSTRAINT fk_feedback_handler FOREIGN KEY (handler_id) REFERENCES sys_user(id),
  INDEX idx_feedback_status (status),
  INDEX idx_feedback_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

