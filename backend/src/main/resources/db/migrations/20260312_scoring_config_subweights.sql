USE student_eval;

-- Non-destructive migration: add per-semester sub-weights for Intel/Sport calculation.
ALTER TABLE scoring_config
  ADD COLUMN IF NOT EXISTS intel_course_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.8500 AFTER w_intel;

ALTER TABLE scoring_config
  ADD COLUMN IF NOT EXISTS intel_innovation_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.1500 AFTER intel_course_ratio;

ALTER TABLE scoring_config
  ADD COLUMN IF NOT EXISTS sport_university_pe_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.8500 AFTER w_sport;

ALTER TABLE scoring_config
  ADD COLUMN IF NOT EXISTS sport_activity_ratio DECIMAL(8,4) NOT NULL DEFAULT 0.1500 AFTER sport_university_pe_ratio;

