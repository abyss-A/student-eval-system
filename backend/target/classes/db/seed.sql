USE student_eval;

SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE review_log;
TRUNCATE TABLE activity_item;
TRUNCATE TABLE course_item;
TRUNCATE TABLE attachment;
TRUNCATE TABLE submission;
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
