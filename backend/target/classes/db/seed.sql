USE student_eval;

-- Business-only reset: keep users, semester, scoring config, notices, feedbacks.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE review_log;
TRUNCATE TABLE activity_item;
TRUNCATE TABLE course_item;
TRUNCATE TABLE attachment;
TRUNCATE TABLE submission;
SET FOREIGN_KEY_CHECKS = 1;

-- Ensure there is an active semester and scoring config.
SET @semester_id := (SELECT id FROM semester WHERE is_active = 1 ORDER BY id DESC LIMIT 1);
SET @semester_id := COALESCE(@semester_id, (SELECT id FROM semester ORDER BY id DESC LIMIT 1));

INSERT INTO semester(name, year_num, term_num, is_active)
SELECT '2026年春季学期', 2026, 1, 1
WHERE @semester_id IS NULL;

SET @semester_id := COALESCE(@semester_id, LAST_INSERT_ID());

UPDATE semester
SET is_active = CASE WHEN id = @semester_id THEN 1 ELSE is_active END;

INSERT INTO scoring_config(
  semester_id, appeal_days, precedence_mode, score_model,
  w_moral, w_intel, w_sport, w_art, w_labor,
  cap_moral, cap_intel, cap_sport, cap_art, cap_labor
)
SELECT @semester_id, 10, 'COLLEGE_FIRST', 'STRICT_FORMULA',
       0.15, 0.60, 0.10, 0.075, 0.075,
       100, 100, 100, 100, 100
WHERE NOT EXISTS (
  SELECT 1 FROM scoring_config WHERE semester_id = @semester_id
);

DROP TEMPORARY TABLE IF EXISTS tmp_seed_students;
CREATE TEMPORARY TABLE tmp_seed_students AS
SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
FROM sys_user
WHERE role = 'STUDENT' AND enabled = 1;

-- 1) DRAFT: partial input, no “大学体育” row.
INSERT INTO submission(
  semester_id, student_id, status,
  moral_raw, intel_raw, sport_raw, art_raw, labor_raw, total_score,
  submitted_at, finalized_at, published_at, created_at, updated_at
)
SELECT @semester_id, id, 'DRAFT',
       0, 0, 0, 0, 0, 0,
       NULL, NULL, NULL, NOW(), NOW()
FROM tmp_seed_students WHERE rn = 1;

SET @sub_draft := (
  SELECT s.id
  FROM submission s JOIN tmp_seed_students t ON s.student_id = t.id
  WHERE s.semester_id = @semester_id AND t.rn = 1
  LIMIT 1
);

INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score, reviewer_comment)
SELECT @sub_draft, '高等数学', 'REQUIRED', 86, 4, 'PENDING', 86, NULL
WHERE @sub_draft IS NOT NULL;

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment)
SELECT @sub_draft, 'SPORT_ACTIVITY', '校园晨跑', '每周晨跑打卡', 18, 18, '', 'PENDING', NULL
WHERE @sub_draft IS NOT NULL;

-- 2) SUBMITTED + NOT_REVIEWED: all items pending, single “大学体育”.
INSERT INTO submission(
  semester_id, student_id, status,
  moral_raw, intel_raw, sport_raw, art_raw, labor_raw, total_score,
  submitted_at, finalized_at, published_at, created_at, updated_at
)
SELECT @semester_id, id, 'SUBMITTED',
       0, 0, 0, 0, 0, 0,
       NOW(), NULL, NULL, NOW(), NOW()
FROM tmp_seed_students WHERE rn = 2;

SET @sub_not_reviewed := (
  SELECT s.id
  FROM submission s JOIN tmp_seed_students t ON s.student_id = t.id
  WHERE s.semester_id = @semester_id AND t.rn = 2
  LIMIT 1
);

INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score, reviewer_comment)
SELECT @sub_not_reviewed, '线性代数', 'REQUIRED', 84, 3, 'PENDING', 84, NULL
WHERE @sub_not_reviewed IS NOT NULL;

INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score, reviewer_comment)
SELECT @sub_not_reviewed, '概率论', 'REQUIRED', 82, 3, 'PENDING', 82, NULL
WHERE @sub_not_reviewed IS NOT NULL;

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment)
SELECT @sub_not_reviewed, 'SPORT_ACTIVITY', '大学体育', '体育课成绩填报', 88, 88, '', 'PENDING', NULL
WHERE @sub_not_reviewed IS NOT NULL;

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment)
SELECT @sub_not_reviewed, 'SPORT_ACTIVITY', '体测1000米', '校体测成绩', 75, 75, '', 'PENDING', NULL
WHERE @sub_not_reviewed IS NOT NULL;

-- 3) SUBMITTED + IN_PROGRESS: reviewed + pending mixed.
INSERT INTO submission(
  semester_id, student_id, status,
  moral_raw, intel_raw, sport_raw, art_raw, labor_raw, total_score,
  submitted_at, finalized_at, published_at, created_at, updated_at
)
SELECT @semester_id, id, 'SUBMITTED',
       0, 0, 0, 0, 0, 0,
       NOW(), NULL, NULL, NOW(), NOW()
FROM tmp_seed_students WHERE rn = 3;

SET @sub_in_progress := (
  SELECT s.id
  FROM submission s JOIN tmp_seed_students t ON s.student_id = t.id
  WHERE s.semester_id = @semester_id AND t.rn = 3
  LIMIT 1
);

INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score, reviewer_comment)
SELECT @sub_in_progress, '离散数学', 'REQUIRED', 87, 3, 'APPROVED', 87, '材料完整'
WHERE @sub_in_progress IS NOT NULL;

INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score, reviewer_comment)
SELECT @sub_in_progress, '数据库原理', 'REQUIRED', 90, 4, 'PENDING', 90, NULL
WHERE @sub_in_progress IS NOT NULL;

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment)
SELECT @sub_in_progress, 'MORAL', '主题团日', '班级组织团日活动', 12, 0, '', 'REJECTED', '缺少证明说明'
WHERE @sub_in_progress IS NOT NULL;

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment)
SELECT @sub_in_progress, 'SPORT_ACTIVITY', '大学体育', '体育课成绩填报', 86, 86, '', 'PENDING', NULL
WHERE @sub_in_progress IS NOT NULL;

-- 4) SUBMITTED + DONE_NEED_STUDENT_FIX: no pending, has rejected.
INSERT INTO submission(
  semester_id, student_id, status,
  moral_raw, intel_raw, sport_raw, art_raw, labor_raw, total_score,
  submitted_at, finalized_at, published_at, created_at, updated_at
)
SELECT @semester_id, id, 'SUBMITTED',
       0, 0, 0, 0, 0, 0,
       NOW(), NULL, NULL, NOW(), NOW()
FROM tmp_seed_students WHERE rn = 4;

SET @sub_need_fix := (
  SELECT s.id
  FROM submission s JOIN tmp_seed_students t ON s.student_id = t.id
  WHERE s.semester_id = @semester_id AND t.rn = 4
  LIMIT 1
);

INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score, reviewer_comment)
SELECT @sub_need_fix, '数值分析', 'REQUIRED', 83, 3, 'APPROVED', 83, '通过'
WHERE @sub_need_fix IS NOT NULL;

INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score, reviewer_comment)
SELECT @sub_need_fix, '复变函数', 'REQUIRED', 76, 3, 'REJECTED', 0, '成绩证明不清晰'
WHERE @sub_need_fix IS NOT NULL;

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment)
SELECT @sub_need_fix, 'SPORT_ACTIVITY', '大学体育', '体育课成绩填报', 90, 90, '', 'APPROVED', '通过'
WHERE @sub_need_fix IS NOT NULL;

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment)
SELECT @sub_need_fix, 'SPORT_ACTIVITY', '院运会100米', '院级田径比赛', 32, 0, '', 'REJECTED', '缺少赛事证明'
WHERE @sub_need_fix IS NOT NULL;

-- 5) COUNSELOR_REVIEWED: all approved.
INSERT INTO submission(
  semester_id, student_id, status,
  moral_raw, intel_raw, sport_raw, art_raw, labor_raw, total_score,
  submitted_at, finalized_at, published_at, created_at, updated_at
)
SELECT @semester_id, id, 'COUNSELOR_REVIEWED',
       0, 0, 0, 0, 0, 0,
       NOW(), NULL, NULL, NOW(), NOW()
FROM tmp_seed_students WHERE rn = 5;

SET @sub_counselor_reviewed := (
  SELECT s.id
  FROM submission s JOIN tmp_seed_students t ON s.student_id = t.id
  WHERE s.semester_id = @semester_id AND t.rn = 5
  LIMIT 1
);

INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score, reviewer_comment)
SELECT @sub_counselor_reviewed, '实变函数', 'REQUIRED', 89, 3, 'APPROVED', 89, '通过'
WHERE @sub_counselor_reviewed IS NOT NULL;

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment)
SELECT @sub_counselor_reviewed, 'SPORT_ACTIVITY', '大学体育', '体育课成绩填报', 91, 91, '', 'APPROVED', '通过'
WHERE @sub_counselor_reviewed IS NOT NULL;

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment)
SELECT @sub_counselor_reviewed, 'SPORT_ACTIVITY', '篮球赛', '学院篮球赛', 20, 20, '', 'APPROVED', '通过'
WHERE @sub_counselor_reviewed IS NOT NULL;

-- 6) FINALIZED.
INSERT INTO submission(
  semester_id, student_id, status,
  moral_raw, intel_raw, sport_raw, art_raw, labor_raw, total_score,
  submitted_at, finalized_at, published_at, created_at, updated_at
)
SELECT @semester_id, id, 'FINALIZED',
       10, 54, 8, 5, 5, 82,
       NOW(), NOW(), NULL, NOW(), NOW()
FROM tmp_seed_students WHERE rn = 6;

SET @sub_finalized := (
  SELECT s.id
  FROM submission s JOIN tmp_seed_students t ON s.student_id = t.id
  WHERE s.semester_id = @semester_id AND t.rn = 6
  LIMIT 1
);

INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score, reviewer_comment)
SELECT @sub_finalized, '高等代数', 'REQUIRED', 92, 4, 'APPROVED', 92, '通过'
WHERE @sub_finalized IS NOT NULL;

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment)
SELECT @sub_finalized, 'SPORT_ACTIVITY', '大学体育', '体育课成绩填报', 93, 93, '', 'APPROVED', '通过'
WHERE @sub_finalized IS NOT NULL;

-- 7) PUBLISHED.
INSERT INTO submission(
  semester_id, student_id, status,
  moral_raw, intel_raw, sport_raw, art_raw, labor_raw, total_score,
  submitted_at, finalized_at, published_at, created_at, updated_at
)
SELECT @semester_id, id, 'PUBLISHED',
       11, 56, 8.5, 6, 6, 87.5,
       NOW(), NOW(), NOW(), NOW(), NOW()
FROM tmp_seed_students WHERE rn = 7;

SET @sub_published := (
  SELECT s.id
  FROM submission s JOIN tmp_seed_students t ON s.student_id = t.id
  WHERE s.semester_id = @semester_id AND t.rn = 7
  LIMIT 1
);

INSERT INTO course_item(submission_id, course_name, course_type, score, credit, review_status, reviewer_score, reviewer_comment)
SELECT @sub_published, '概率统计', 'REQUIRED', 90, 3, 'APPROVED', 90, '通过'
WHERE @sub_published IS NOT NULL;

INSERT INTO activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment)
SELECT @sub_published, 'SPORT_ACTIVITY', '大学体育', '体育课成绩填报', 88, 88, '', 'APPROVED', '通过'
WHERE @sub_published IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_students;
