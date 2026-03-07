USE student_eval;
SET NAMES utf8mb4;

-- Reuse existing password hashes when present so existing login habits remain unchanged.
SET @pwd_admin := COALESCE(
  (SELECT password_hash FROM sys_user WHERE account_no = '9000000001' LIMIT 1),
  '$2a$10$FiYZO6eqXBjrSGGTRr9S0.HHIwNo0C2mshhDeTL3Rf9YztgkXIeWy'
);
SET @pwd_counselor := COALESCE(
  (SELECT password_hash FROM sys_user WHERE account_no = '9000000002' LIMIT 1),
  '$2a$10$zksDvfoaSurLTcqCdMWxV.Of2aYnAzqQystpBLStqbTZis1vifjh.'
);
SET @pwd_student := COALESCE(
  (SELECT password_hash FROM sys_user WHERE account_no = '2022000001' LIMIT 1),
  '$2a$10$2XQupALSn46XWqZu/iL0Cu/URUqzh6kI0WGHdBJBvPjU1Bxrg3vQW'
);

INSERT INTO sys_user(password_hash, role, account_no, real_name, gender, phone, class_name, enabled)
VALUES
  -- admins
  (@pwd_admin, 'ADMIN', '9000000001', '系统管理员', '男', '13800000001', NULL, 1),

  -- counselors
  (@pwd_counselor, 'COUNSELOR', '9000000002', '张老师', '男', '13800000002', NULL, 1),
  (@pwd_counselor, 'COUNSELOR', '9000000003', '李老师', '女', '13800000003', NULL, 1),
  (@pwd_counselor, 'COUNSELOR', '9000000004', '王老师', '男', '13800000004', NULL, 1),
  (@pwd_counselor, 'COUNSELOR', '9000000005', '陈老师', '女', '13800000005', NULL, 1),

  -- existing 2022 students normalized to "年级+专业+班级"
  (@pwd_student, 'STUDENT', '2022000001', '学生001', '男', '13920000001', '2022级数据科学与大数据技术1班', 1),
  (@pwd_student, 'STUDENT', '2022000002', '学生002', '女', '13920000002', '2022级数据科学与大数据技术2班', 1),
  (@pwd_student, 'STUDENT', '2022000003', '学生003', '男', '13920000003', '2022级信息与计算科学1班', 1),
  (@pwd_student, 'STUDENT', '2022000004', '学生004', '女', '13920000004', '2022级信息与计算科学2班', 1),
  (@pwd_student, 'STUDENT', '2022000005', '学生005', '男', '13920000005', '2022级数学与应用数学（师范）1班', 1),
  (@pwd_student, 'STUDENT', '2022000006', '学生006', '女', '13920000006', '2022级数学与应用数学（师范）2班', 1),
  (@pwd_student, 'STUDENT', '2022000007', '学生007', '男', '13920000007', '2022级数学与应用数学（非师范）1班', 1),
  (@pwd_student, 'STUDENT', '2022000008', '学生008', '女', '13920000008', '2022级数学与应用数学（非师范）2班', 1),
  (@pwd_student, 'STUDENT', '2022000009', '学生009', '男', '13920000009', '2022级数据科学与大数据技术2班', 1),
  (@pwd_student, 'STUDENT', '2022000010', '学生010', '女', '13920000010', '2022级信息与计算科学2班', 1),
  (@pwd_student, 'STUDENT', '2210601237', '冷建华', '男', '13920000137', '2022级数据科学与大数据技术2班', 1),

  -- 2023 cohort
  (@pwd_student, 'STUDENT', '2023000001', '学生011', '男', '13930000001', '2023级数据科学与大数据技术1班', 1),
  (@pwd_student, 'STUDENT', '2023000002', '学生012', '女', '13930000002', '2023级数据科学与大数据技术2班', 1),
  (@pwd_student, 'STUDENT', '2023000003', '学生013', '男', '13930000003', '2023级信息与计算科学1班', 1),
  (@pwd_student, 'STUDENT', '2023000004', '学生014', '女', '13930000004', '2023级信息与计算科学2班', 1),
  (@pwd_student, 'STUDENT', '2023000005', '学生015', '男', '13930000005', '2023级数学与应用数学（师范）1班', 1),
  (@pwd_student, 'STUDENT', '2023000006', '学生016', '女', '13930000006', '2023级数学与应用数学（师范）2班', 1),
  (@pwd_student, 'STUDENT', '2023000007', '学生017', '男', '13930000007', '2023级数学与应用数学（非师范）1班', 1),
  (@pwd_student, 'STUDENT', '2023000008', '学生018', '女', '13930000008', '2023级数学与应用数学（非师范）2班', 1),
  (@pwd_student, 'STUDENT', '2023000009', '学生019', '男', '13930000009', '2023级数据科学与大数据技术1班', 1),
  (@pwd_student, 'STUDENT', '2023000010', '学生020', '女', '13930000010', '2023级信息与计算科学1班', 1),
  (@pwd_student, 'STUDENT', '2023000011', '学生021', '男', '13930000011', '2023级数学与应用数学（师范）1班', 1),
  (@pwd_student, 'STUDENT', '2023000012', '学生022', '女', '13930000012', '2023级数学与应用数学（非师范）1班', 1),

  -- 2024 cohort
  (@pwd_student, 'STUDENT', '2024000001', '学生023', '男', '13940000001', '2024级数据科学与大数据技术1班', 1),
  (@pwd_student, 'STUDENT', '2024000002', '学生024', '女', '13940000002', '2024级数据科学与大数据技术2班', 1),
  (@pwd_student, 'STUDENT', '2024000003', '学生025', '男', '13940000003', '2024级信息与计算科学1班', 1),
  (@pwd_student, 'STUDENT', '2024000004', '学生026', '女', '13940000004', '2024级信息与计算科学2班', 1),
  (@pwd_student, 'STUDENT', '2024000005', '学生027', '男', '13940000005', '2024级数学与应用数学（师范）1班', 1),
  (@pwd_student, 'STUDENT', '2024000006', '学生028', '女', '13940000006', '2024级数学与应用数学（师范）2班', 1),
  (@pwd_student, 'STUDENT', '2024000007', '学生029', '男', '13940000007', '2024级数学与应用数学（非师范）1班', 1),
  (@pwd_student, 'STUDENT', '2024000008', '学生030', '女', '13940000008', '2024级数学与应用数学（非师范）2班', 1),
  (@pwd_student, 'STUDENT', '2024000009', '学生031', '男', '13940000009', '2024级数据科学与大数据技术1班', 1),
  (@pwd_student, 'STUDENT', '2024000010', '学生032', '女', '13940000010', '2024级信息与计算科学1班', 1),
  (@pwd_student, 'STUDENT', '2024000011', '学生033', '男', '13940000011', '2024级数学与应用数学（师范）1班', 1),
  (@pwd_student, 'STUDENT', '2024000012', '学生034', '女', '13940000012', '2024级数学与应用数学（非师范）1班', 1),

  -- 2025 cohort
  (@pwd_student, 'STUDENT', '2025000001', '学生035', '男', '13950000001', '2025级数据科学与大数据技术1班', 1),
  (@pwd_student, 'STUDENT', '2025000002', '学生036', '女', '13950000002', '2025级信息与计算科学1班', 1),
  (@pwd_student, 'STUDENT', '2025000003', '学生037', '男', '13950000003', '2025级数学与应用数学（师范）1班', 1),
  (@pwd_student, 'STUDENT', '2025000004', '学生038', '女', '13950000004', '2025级数学与应用数学（非师范）1班', 1)
ON DUPLICATE KEY UPDATE
  role = VALUES(role),
  real_name = VALUES(real_name),
  gender = VALUES(gender),
  phone = VALUES(phone),
  class_name = VALUES(class_name),
  enabled = 1,
  password_hash = CASE
    WHEN sys_user.password_hash IS NULL OR sys_user.password_hash = '' THEN VALUES(password_hash)
    ELSE sys_user.password_hash
  END;

-- Fallback normalization for legacy generic class names like "2026级2班".
UPDATE sys_user
SET class_name = CONCAT(
  SUBSTRING_INDEX(class_name, '级', 1),
  '级数据科学与大数据技术',
  REPLACE(SUBSTRING_INDEX(class_name, '级', -1), '班', ''),
  '班'
)
WHERE role = 'STUDENT'
  AND class_name REGEXP '^[0-9]{4}级[0-9]+班$';

-- Drop old coarse scopes before writing normalized scopes.
DELETE FROM counselor_class_scope
WHERE class_name REGEXP '^[0-9]{4}级[0-9]+班$'
   OR class_name = '2022级大数据2班';

INSERT INTO counselor_class_scope(counselor_id, class_name, assigned_by, created_at, updated_at)
SELECT c.id, t.class_name, a.id, NOW(), NOW()
FROM (
  SELECT '9000000002' AS counselor_no, '2022级数据科学与大数据技术1班' AS class_name
  UNION ALL SELECT '9000000002', '2022级数据科学与大数据技术2班'
  UNION ALL SELECT '9000000002', '2022级信息与计算科学1班'
  UNION ALL SELECT '9000000002', '2022级信息与计算科学2班'
  UNION ALL SELECT '9000000002', '2023级数据科学与大数据技术1班'
  UNION ALL SELECT '9000000002', '2023级数据科学与大数据技术2班'
  UNION ALL SELECT '9000000002', '2023级信息与计算科学1班'
  UNION ALL SELECT '9000000002', '2023级信息与计算科学2班'

  UNION ALL SELECT '9000000003', '2022级数学与应用数学（师范）1班'
  UNION ALL SELECT '9000000003', '2022级数学与应用数学（师范）2班'
  UNION ALL SELECT '9000000003', '2022级数学与应用数学（非师范）1班'
  UNION ALL SELECT '9000000003', '2022级数学与应用数学（非师范）2班'
  UNION ALL SELECT '9000000003', '2023级数学与应用数学（师范）1班'
  UNION ALL SELECT '9000000003', '2023级数学与应用数学（师范）2班'
  UNION ALL SELECT '9000000003', '2023级数学与应用数学（非师范）1班'
  UNION ALL SELECT '9000000003', '2023级数学与应用数学（非师范）2班'

  UNION ALL SELECT '9000000004', '2024级数据科学与大数据技术1班'
  UNION ALL SELECT '9000000004', '2024级数据科学与大数据技术2班'
  UNION ALL SELECT '9000000004', '2024级信息与计算科学1班'
  UNION ALL SELECT '9000000004', '2024级信息与计算科学2班'
  UNION ALL SELECT '9000000004', '2025级数据科学与大数据技术1班'
  UNION ALL SELECT '9000000004', '2025级信息与计算科学1班'

  UNION ALL SELECT '9000000005', '2024级数学与应用数学（师范）1班'
  UNION ALL SELECT '9000000005', '2024级数学与应用数学（师范）2班'
  UNION ALL SELECT '9000000005', '2024级数学与应用数学（非师范）1班'
  UNION ALL SELECT '9000000005', '2024级数学与应用数学（非师范）2班'
  UNION ALL SELECT '9000000005', '2025级数学与应用数学（师范）1班'
  UNION ALL SELECT '9000000005', '2025级数学与应用数学（非师范）1班'
) t
JOIN sys_user c ON c.account_no = t.counselor_no AND c.role = 'COUNSELOR'
JOIN sys_user a ON a.account_no = '9000000001' AND a.role = 'ADMIN'
ON DUPLICATE KEY UPDATE
  counselor_id = VALUES(counselor_id),
  assigned_by = VALUES(assigned_by),
  updated_at = NOW();
