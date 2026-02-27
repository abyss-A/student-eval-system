USE student_eval;

SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE review_log;
TRUNCATE TABLE activity_item;
TRUNCATE TABLE course_item;
TRUNCATE TABLE attachment;
TRUNCATE TABLE submission;
TRUNCATE TABLE feedback;
TRUNCATE TABLE notice;
TRUNCATE TABLE scoring_config;
TRUNCATE TABLE semester;
TRUNCATE TABLE sys_user;
SET FOREIGN_KEY_CHECKS=1;

-- Semester & scoring config
INSERT INTO semester(id,name,year_num,term_num,is_active)
VALUES (1, CONVERT(0x32303236e5b9b4e698a5e5ada3e5ada6e69c9f USING utf8mb4), 2026, 1, 1);

INSERT INTO scoring_config(
  semester_id, appeal_days, precedence_mode, score_model,
  w_moral, w_intel, w_sport, w_art, w_labor,
  cap_moral, cap_intel, cap_sport, cap_art, cap_labor
) VALUES (1, 10, 'COLLEGE_FIRST', 'STRICT_FORMULA', 0.15, 0.60, 0.10, 0.075, 0.075, 100, 100, 100, 100, 100);

-- Accounts (username/password should be ASCII; names/classes/majors are Chinese in DB)
INSERT INTO sys_user(id, username, password_hash, role, real_name, enabled)
VALUES (1, 'admin', '123456', 'ADMIN', CONVERT(0xe7b3bbe7bb9fe7aea1e79086e59198 USING utf8mb4), 1);

INSERT INTO sys_user(id, username, password_hash, role, real_name, enabled)
VALUES (2, 'counselor1', '123456', 'COUNSELOR', CONVERT(0xe5bca0e88081e5b888 USING utf8mb4), 1);

INSERT INTO sys_user(id, username, password_hash, role, student_no, real_name, class_name, major_name, enabled)
VALUES (3, 'stu0001', '123456', 'STUDENT', '2022000001', CONVERT(0xe5ada6e7949f303031 USING utf8mb4), CONVERT(0x32303232e7baa731e78fad USING utf8mb4), CONVERT(0xe695b0e5ada6e4b88ee5ba94e794a8e695b0e5ada6 USING utf8mb4), 1);
INSERT INTO sys_user(id, username, password_hash, role, student_no, real_name, class_name, major_name, enabled)
VALUES (4, 'stu0002', '123456', 'STUDENT', '2022000002', CONVERT(0xe5ada6e7949f303032 USING utf8mb4), CONVERT(0x32303232e7baa732e78fad USING utf8mb4), CONVERT(0xe695b0e5ada6e4b88ee5ba94e794a8e695b0e5ada6 USING utf8mb4), 1);
INSERT INTO sys_user(id, username, password_hash, role, student_no, real_name, class_name, major_name, enabled)
VALUES (5, 'stu0003', '123456', 'STUDENT', '2022000003', CONVERT(0xe5ada6e7949f303033 USING utf8mb4), CONVERT(0x32303232e7baa733e78fad USING utf8mb4), CONVERT(0xe695b0e5ada6e4b88ee5ba94e794a8e695b0e5ada6 USING utf8mb4), 1);
INSERT INTO sys_user(id, username, password_hash, role, student_no, real_name, class_name, major_name, enabled)
VALUES (6, 'stu0004', '123456', 'STUDENT', '2022000004', CONVERT(0xe5ada6e7949f303034 USING utf8mb4), CONVERT(0x32303232e7baa731e78fad USING utf8mb4), CONVERT(0xe695b0e5ada6e4b88ee5ba94e794a8e695b0e5ada6 USING utf8mb4), 1);
INSERT INTO sys_user(id, username, password_hash, role, student_no, real_name, class_name, major_name, enabled)
VALUES (7, 'stu0005', '123456', 'STUDENT', '2022000005', CONVERT(0xe5ada6e7949f303035 USING utf8mb4), CONVERT(0x32303232e7baa732e78fad USING utf8mb4), CONVERT(0xe695b0e5ada6e4b88ee5ba94e794a8e695b0e5ada6 USING utf8mb4), 1);
INSERT INTO sys_user(id, username, password_hash, role, student_no, real_name, class_name, major_name, enabled)
VALUES (8, 'stu0006', '123456', 'STUDENT', '2022000006', CONVERT(0xe5ada6e7949f303036 USING utf8mb4), CONVERT(0x32303232e7baa733e78fad USING utf8mb4), CONVERT(0xe695b0e5ada6e4b88ee5ba94e794a8e695b0e5ada6 USING utf8mb4), 1);
INSERT INTO sys_user(id, username, password_hash, role, student_no, real_name, class_name, major_name, enabled)
VALUES (9, 'stu0007', '123456', 'STUDENT', '2022000007', CONVERT(0xe5ada6e7949f303037 USING utf8mb4), CONVERT(0x32303232e7baa731e78fad USING utf8mb4), CONVERT(0xe695b0e5ada6e4b88ee5ba94e794a8e695b0e5ada6 USING utf8mb4), 1);
INSERT INTO sys_user(id, username, password_hash, role, student_no, real_name, class_name, major_name, enabled)
VALUES (10, 'stu0008', '123456', 'STUDENT', '2022000008', CONVERT(0xe5ada6e7949f303038 USING utf8mb4), CONVERT(0x32303232e7baa732e78fad USING utf8mb4), CONVERT(0xe695b0e5ada6e4b88ee5ba94e794a8e695b0e5ada6 USING utf8mb4), 1);
INSERT INTO sys_user(id, username, password_hash, role, student_no, real_name, class_name, major_name, enabled)
VALUES (11, 'stu0009', '123456', 'STUDENT', '2022000009', CONVERT(0xe5ada6e7949f303039 USING utf8mb4), CONVERT(0x32303232e7baa733e78fad USING utf8mb4), CONVERT(0xe695b0e5ada6e4b88ee5ba94e794a8e695b0e5ada6 USING utf8mb4), 1);
INSERT INTO sys_user(id, username, password_hash, role, student_no, real_name, class_name, major_name, enabled)
VALUES (12, 'stu0010', '123456', 'STUDENT', '2022000010', CONVERT(0xe5ada6e7949f303130 USING utf8mb4), CONVERT(0x32303232e7baa731e78fad USING utf8mb4), CONVERT(0xe695b0e5ada6e4b88ee5ba94e794a8e695b0e5ada6 USING utf8mb4), 1);

-- Example submission (for ranking/export demo)
INSERT INTO submission(
  id, semester_id, student_id, status,
  moral_raw, intel_raw, sport_raw, art_raw, labor_raw,
  total_score, submitted_at, created_at, updated_at
) VALUES (1, 1, 3, 'SUBMITTED', 30, 75.56, 69.50, 10, 10, 58.28, NOW(), NOW(), NOW());

INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score)
VALUES (1, CONVERT(0xe9ab98e7ad89e4bba3e695b0 USING utf8mb4), 'REQUIRED', 86, 4, 'APPROVED', 86);
INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score)
VALUES (1, CONVERT(0xe695b0e5ada6e58886e69e90 USING utf8mb4), 'REQUIRED', 88, 5, 'APPROVED', 88);
INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score)
VALUES (1, CONVERT(0xe5a4a7e5ada6e4bd93e882b2 USING utf8mb4), 'REQUIRED', 80, 2, 'APPROVED', 80);
INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score)
VALUES (1, CONVERT(0xe6a682e78e87e8aeba USING utf8mb4), 'REQUIRED', 90, 5, 'APPROVED', 90);

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status)
VALUES (1, 'MORAL', CONVERT(0xe4b8bbe9a298e59ba2e697a5 USING utf8mb4), CONVERT(0xe58f82e4b88ee6b4bbe58aa8 USING utf8mb4), 10, 10, '', 'APPROVED');
INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status)
VALUES (1, 'MORAL', CONVERT(0xe695b0e5ada6e5bbbae6a8a1e7a4bee68890e59198 USING utf8mb4), '', 20, 20, '', 'APPROVED');
INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status)
VALUES (1, 'INTEL_PRO_INNOV', CONVERT(0xe5ada6e7a791e7ab9ee8b59be88eb7e5a596 USING utf8mb4), '', 10, 10, '', 'APPROVED');
INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status)
VALUES (1, 'SPORT_ACTIVITY', CONVERT(0xe8bf90e58aa8e4bc9a USING utf8mb4), '', 10, 10, '', 'APPROVED');
INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status)
VALUES (1, 'ART', CONVERT(0xe5be81e69687e6af94e8b59b USING utf8mb4), '', 10, 10, '', 'APPROVED');
INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status)
VALUES (1, 'LABOR', CONVERT(0xe58ab3e58aa8e5ae9ee8b7b5 USING utf8mb4), '', 10, 10, '', 'APPROVED');

-- Notices (announcement)
INSERT INTO notice(id, title, content, status, publisher_id, published_at, offline_at, created_at, updated_at)
VALUES (
  1,
  CONVERT(0xe585b3e4ba8e32303236e5b9b4e698a5e5ada3e7bbbce59088e6b58be8af84e5a1abe68aa5e79a84e9809ae79fa5 USING utf8mb4),
  CONVERT(0xe59084e4bd8de5908ce5ada6efbc9a0ae8afb7e4ba8e33e69c883130e697a5e5898de5ae8ce68890e7bbbce59088e6b58be8af84e5a1abe68aa5efbc8ce980bee69c9fe4b88de4ba88e58f97e79086e380820ae8af81e6988ee69d90e69699e4bb85e694afe68c814a50472f504e47e59bbee78987e38082 USING utf8mb4),
  'DRAFT',
  2,
  NULL,
  NULL,
  NOW(),
  NOW()
);

INSERT INTO notice(id, title, content, status, publisher_id, published_at, offline_at, created_at, updated_at)
VALUES (
  2,
  CONVERT(0xe7bbbce59088e6b58be8af84e7b3bbe7bb9fe8af95e8bf90e8a18ce8afb4e6988e USING utf8mb4),
  CONVERT(0xe69cace7b3bbe7bb9fe794a8e4ba8ee5ada6e7949fe7bbbce59088e6b58be8af84e5a1abe68aa5e38081e8be85e5afbce59198e5aea1e6a0b8e38081e7aea1e79086e59198e7bb88e5aea1e4b88ee68e92e5908de585ace7a4bae380820ae5a682e98187e588b0e997aee9a298efbc8ce8afb7e59ca8e38090e58f8de9a688e7aea1e79086e38091e4b8ade68f90e4baa4e58f8de9a688e38082 USING utf8mb4),
  'PUBLISHED',
  2,
  NOW(),
  NULL,
  NOW(),
  NOW()
);

INSERT INTO notice(id, title, content, status, publisher_id, published_at, offline_at, created_at, updated_at)
VALUES (
  3,
  CONVERT(0xe5b7b2e4b88be7babfe7a4bae4be8be585ace5918a USING utf8mb4),
  CONVERT(0xe8bf99e698afe4b880e69da1e5b7b2e4b88be7babfe79a84e585ace5918ae7a4bae4be8befbc8ce4bb85e794a8e4ba8ee6b58be8af95e38082 USING utf8mb4),
  'OFFLINE',
  1,
  NOW(),
  NOW(),
  NOW(),
  NOW()
);

-- Feedbacks
INSERT INTO feedback(id, creator_id, title, content, screenshot_file_ids, status, created_at, updated_at)
VALUES (
  1,
  3,
  CONVERT(0xe7b3bbe7bb9fe697a0e6b395e4b88ae4bca0e59bbee78987 USING utf8mb4),
  CONVERT(0xe68891e4b88ae4bca0e8af81e6988ee69d90e69699e697b6e68f90e7a4bae6a0bce5bc8fe4b88de694afe68c81efbc8ce4bd86e68891e4b88ae4bca0e79a84e698af6a7067e59bbee78987e38082e8afb7e5b8aee5bf99e69fa5e79c8be38082 USING utf8mb4),
  '',
  'NEW',
  NOW(),
  NOW()
);

INSERT INTO feedback(id, creator_id, title, content, screenshot_file_ids, status, handler_id, reply_content, replied_at, created_at, updated_at)
VALUES (
  2,
  4,
  CONVERT(0xe8afbee7a88be68890e7bba9e4bf9de5ad98e5908ee698bee7a4bae4b8bae7a9ba USING utf8mb4),
  CONVERT(0xe68891e5a1abe4ba86e587a0e997a8e8afbee7a88befbc8ce782b9e587bbe4bf9de5ad98e5908ee588b7e696b0e9a1b5e99da2e698bee7a4bae4b8bae7a9bae38082 USING utf8mb4),
  '',
  'REPLIED',
  2,
  CONVERT(0xe5b7b2e694b6e588b0e58f8de9a688efbc9ae8afb7e58588e7a1aee8aea4e698afe590a6e5b7b2e782b9e587bbe38090e4bf9de5ad98e8afbee7a88be38091e68c89e992aeefbc9be88ba5e4bb8de5bc82e5b8b8efbc8ce8afb7e688aae59bbee5b9b6e5868de6aca1e58f8de9a688e38082 USING utf8mb4),
  NOW(),
  NOW(),
  NOW()
);

INSERT INTO feedback(id, creator_id, title, content, screenshot_file_ids, status, handler_id, reply_content, replied_at, closed_at, created_at, updated_at)
VALUES (
  3,
  5,
  CONVERT(0xe5bbbae8aeaee4bc98e58c96e9a1b5e99da2e9858de889b2 USING utf8mb4),
  CONVERT(0xe9a1b5e99da2e68c89e992aee9a29ce889b2e69c89e782b9e5a49aefbc8ce5b88ce69c9be695b4e4bd93e69bb4e5838fe5ada6e6a0a1e69599e58aa1e7b3bbe7bb9fe9a38ee6a0bce38082 USING utf8mb4),
  '',
  'CLOSED',
  1,
  CONVERT(0xe5b7b2e8aeb0e5bd95e5bbbae8aeaeefbc8ce5908ee7bbade4bc9ae7bb9fe4b880e889b2e8b083e5b9b6e5878fe5b091e9a29ce889b2e7a78de7b1bbe38082e6849fe8b0a2e58f8de9a688e38082 USING utf8mb4),
  NOW(),
  NOW(),
  NOW(),
  NOW()
);
