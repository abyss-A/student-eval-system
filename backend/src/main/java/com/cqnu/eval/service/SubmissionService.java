package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.*;
import com.cqnu.eval.model.dto.ActivityItemInput;
import com.cqnu.eval.model.dto.BatchActivityRequest;
import com.cqnu.eval.model.dto.BatchCourseRequest;
import com.cqnu.eval.model.dto.CourseItemInput;
import com.cqnu.eval.model.entity.*;
import com.cqnu.eval.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SubmissionService {

    private final SemesterMapper semesterMapper;
    private final SubmissionMapper submissionMapper;
    private final CourseItemMapper courseItemMapper;
    private final ActivityItemMapper activityItemMapper;
    private final UserMapper userMapper;
    private final ScoringConfigMapper scoringConfigMapper;

    public SubmissionService(SemesterMapper semesterMapper,
                             SubmissionMapper submissionMapper,
                             CourseItemMapper courseItemMapper,
                             ActivityItemMapper activityItemMapper,
                             UserMapper userMapper,
                             ScoringConfigMapper scoringConfigMapper) {
        this.semesterMapper = semesterMapper;
        this.submissionMapper = submissionMapper;
        this.courseItemMapper = courseItemMapper;
        this.activityItemMapper = activityItemMapper;
        this.userMapper = userMapper;
        this.scoringConfigMapper = scoringConfigMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmissionEntity createOrGetMySubmission(Long studentId) {
        SemesterEntity active = semesterMapper.findActive();
        if (active == null) {
            throw new BizException(40401, "当前没有激活学期");
        }
        SubmissionEntity existing = submissionMapper.findBySemesterAndStudent(active.getId(), studentId);
        if (existing != null) {
            return existing;
        }
        SubmissionEntity entity = new SubmissionEntity();
        entity.setSemesterId(active.getId());
        entity.setStudentId(studentId);
        entity.setStatus("DRAFT");
        entity.setMoralRaw(BigDecimal.ZERO);
        entity.setIntelRaw(BigDecimal.ZERO);
        entity.setSportRaw(BigDecimal.ZERO);
        entity.setArtRaw(BigDecimal.ZERO);
        entity.setLaborRaw(BigDecimal.ZERO);
        entity.setTotalScore(BigDecimal.ZERO);
        submissionMapper.insert(entity);
        return entity;
    }

    public Map<String, Object> getSubmissionDetail(Long submissionId, CurrentUser currentUser) {
        SubmissionEntity submission = submissionMapper.findById(submissionId);
        if (submission == null) {
            throw new BizException(40401, "测评单不存在");
        }
        if ("STUDENT".equalsIgnoreCase(currentUser.getRole())
                && submissionMapper.checkOwner(submissionId, currentUser.getId()) == 0) {
            throw new BizException(40301, "无权访问该测评单");
        }
        UserEntity student = userMapper.findById(submission.getStudentId());
        SemesterEntity semester = semesterMapper.findActive();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("submission", submission);
        data.put("student", student);
        data.put("semester", semester);
        data.put("courses", courseItemMapper.listBySubmissionId(submissionId));
        data.put("activities", activityItemMapper.listBySubmissionId(submissionId));
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveCourses(Long submissionId, Long studentId, BatchCourseRequest request) {
        checkSubmissionEditableByStudent(submissionId, studentId);
        courseItemMapper.deleteBySubmissionId(submissionId);
        for (CourseItemInput item : request.getItems()) {
            CourseItemEntity entity = new CourseItemEntity();
            entity.setSubmissionId(submissionId);
            entity.setCourseName(item.getCourseName());
            entity.setCourseType(item.getCourseType());
            entity.setScore(item.getScore());
            entity.setCredit(item.getCredit());
            entity.setEvidenceFileId(item.getEvidenceFileId());
            entity.setReviewStatus("PENDING");
            entity.setReviewerScore(item.getScore());
            entity.setReviewerComment(null);
            courseItemMapper.insert(entity);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveActivities(Long submissionId, Long studentId, BatchActivityRequest request) {
        checkSubmissionEditableByStudent(submissionId, studentId);
        activityItemMapper.deleteBySubmissionId(submissionId);
        for (ActivityItemInput item : request.getItems()) {
            ActivityItemEntity entity = new ActivityItemEntity();
            entity.setSubmissionId(submissionId);
            entity.setModuleType(item.getModuleType());
            entity.setTitle(item.getTitle());
            entity.setDescription(item.getDescription());
            entity.setSelfScore(item.getSelfScore());
            entity.setFinalScore(item.getSelfScore());
            entity.setEvidenceFileIds(item.getEvidenceFileIds());
            entity.setReviewStatus("PENDING");
            entity.setReviewerComment(null);
            activityItemMapper.insert(entity);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmissionEntity submit(Long submissionId, Long studentId) {
        checkSubmissionEditableByStudent(submissionId, studentId);
        SubmissionEntity entity = submissionMapper.findById(submissionId);
        ScoreResult score = calculateScore(submissionId);
        entity.setStatus("SUBMITTED");
        entity.setMoralRaw(score.getMoralRaw());
        entity.setIntelRaw(score.getIntelRaw());
        entity.setSportRaw(score.getSportRaw());
        entity.setArtRaw(score.getArtRaw());
        entity.setLaborRaw(score.getLaborRaw());
        entity.setTotalScore(score.getTotalScore());
        entity.setSubmittedAt(LocalDateTime.now());
        submissionMapper.updateScoresAndStatus(entity);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmissionEntity finalizeSubmission(Long submissionId) {
        SubmissionEntity entity = submissionMapper.findById(submissionId);
        if (entity == null) {
            throw new BizException(40401, "测评单不存在");
        }
        ScoreResult score = calculateScore(submissionId);
        entity.setStatus("FINALIZED");
        entity.setMoralRaw(score.getMoralRaw());
        entity.setIntelRaw(score.getIntelRaw());
        entity.setSportRaw(score.getSportRaw());
        entity.setArtRaw(score.getArtRaw());
        entity.setLaborRaw(score.getLaborRaw());
        entity.setTotalScore(score.getTotalScore());
        entity.setFinalizedAt(LocalDateTime.now());
        submissionMapper.updateScoresAndStatus(entity);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmissionEntity recalculate(Long submissionId) {
        SubmissionEntity entity = submissionMapper.findById(submissionId);
        if (entity == null) {
            throw new BizException(40401, "测评单不存在");
        }
        ScoreResult score = calculateScore(submissionId);
        entity.setMoralRaw(score.getMoralRaw());
        entity.setIntelRaw(score.getIntelRaw());
        entity.setSportRaw(score.getSportRaw());
        entity.setArtRaw(score.getArtRaw());
        entity.setLaborRaw(score.getLaborRaw());
        entity.setTotalScore(score.getTotalScore());
        submissionMapper.updateScoresAndStatus(entity);
        return entity;
    }

    public Map<String, Object> getScore(Long submissionId, CurrentUser currentUser) {
        SubmissionEntity entity = submissionMapper.findById(submissionId);
        if (entity == null) {
            throw new BizException(40401, "测评单不存在");
        }
        if ("STUDENT".equalsIgnoreCase(currentUser.getRole())
                && submissionMapper.checkOwner(submissionId, currentUser.getId()) == 0) {
            throw new BizException(40301, "无权限");
        }
        ScoreResult result = calculateScore(submissionId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("submissionId", submissionId);
        map.put("status", entity.getStatus());
        map.put("moralRaw", result.getMoralRaw());
        map.put("intelRaw", result.getIntelRaw());
        map.put("sportRaw", result.getSportRaw());
        map.put("artRaw", result.getArtRaw());
        map.put("laborRaw", result.getLaborRaw());
        map.put("totalScore", result.getTotalScore());
        map.put("courseAvg", result.getCourseAvg());
        map.put("formula", "德*15%+智*60%+体*10%+美*7.5%+劳*7.5%");
        return map;
    }

    public List<Map<String, Object>> getRanking(Long semesterId) {
        List<Map<String, Object>> all = submissionMapper.listForRanking(semesterId);

        Map<String, Integer> classCounter = new HashMap<>();
        Map<String, Integer> majorCounter = new HashMap<>();
        for (Map<String, Object> row : all) {
            String className = String.valueOf(row.get("class_name"));
            String majorName = String.valueOf(row.get("major_name"));
            int c = classCounter.getOrDefault(className, 0) + 1;
            int m = majorCounter.getOrDefault(majorName, 0) + 1;
            classCounter.put(className, c);
            majorCounter.put(majorName, m);
            row.put("rankClass", c);
            row.put("rankMajor", m);
        }
        return all;
    }

    private void checkSubmissionEditableByStudent(Long submissionId, Long studentId) {
        SubmissionEntity entity = submissionMapper.findById(submissionId);
        if (entity == null) {
            throw new BizException(40401, "测评单不存在");
        }
        if (submissionMapper.checkOwner(submissionId, studentId) == 0) {
            throw new BizException(40301, "无权限");
        }
        if ("FINALIZED".equals(entity.getStatus()) || "PUBLISHED".equals(entity.getStatus())) {
            throw new BizException(40003, "当前状态不可编辑");
        }
    }

    public ScoreResult calculateScore(Long submissionId) {
        SubmissionEntity submission = submissionMapper.findById(submissionId);
        if (submission == null) {
            throw new BizException(40401, "测评单不存在");
        }

        ScoringConfigEntity cfg = scoringConfigMapper.findBySemesterId(submission.getSemesterId());
        if (cfg == null) {
            cfg = defaultConfig();
        }

        List<CourseItemEntity> courses = courseItemMapper.listBySubmissionId(submissionId);
        List<ActivityItemEntity> activities = activityItemMapper.listBySubmissionId(submissionId);

        BigDecimal creditSum = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal sportCourseCreditSum = BigDecimal.ZERO;
        BigDecimal sportCourseWeighted = BigDecimal.ZERO;

        for (CourseItemEntity c : courses) {
            BigDecimal score = safe(c.getReviewerScore() == null ? c.getScore() : c.getReviewerScore());
            BigDecimal credit = safe(c.getCredit());
            creditSum = creditSum.add(credit);
            weightedSum = weightedSum.add(score.multiply(credit));
            if (c.getCourseName() != null && c.getCourseName().contains("体育")) {
                sportCourseCreditSum = sportCourseCreditSum.add(credit);
                sportCourseWeighted = sportCourseWeighted.add(score.multiply(credit));
            }
        }

        BigDecimal courseAvg = creditSum.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : weightedSum.divide(creditSum, 4, RoundingMode.HALF_UP);

        BigDecimal sportCourseAvg = sportCourseCreditSum.compareTo(BigDecimal.ZERO) == 0
                ? new BigDecimal("60")
                : sportCourseWeighted.divide(sportCourseCreditSum, 4, RoundingMode.HALF_UP);

        BigDecimal moral = BigDecimal.ZERO;
        BigDecimal intelInnovation = BigDecimal.ZERO;
        BigDecimal sportActivity = BigDecimal.ZERO;
        BigDecimal art = BigDecimal.ZERO;
        BigDecimal labor = BigDecimal.ZERO;

        for (ActivityItemEntity a : activities) {
            BigDecimal score = safe(a.getFinalScore() == null ? a.getSelfScore() : a.getFinalScore());
            String module = (a.getModuleType() == null ? "" : a.getModuleType().trim().toUpperCase(Locale.ROOT));
            switch (module) {
                case "MORAL":
                    moral = moral.add(score);
                    break;
                case "INTEL_PRO_INNOV":
                    intelInnovation = intelInnovation.add(score);
                    break;
                case "SPORT_ACTIVITY":
                    sportActivity = sportActivity.add(score);
                    break;
                case "ART":
                    art = art.add(score);
                    break;
                case "LABOR":
                    labor = labor.add(score);
                    break;
                default:
                    break;
            }
        }

        BigDecimal moralRaw = min(moral, BigDecimal.valueOf(cfg.getCapMoral()));
        BigDecimal intelCap = min(intelInnovation, BigDecimal.valueOf(cfg.getCapIntel()));
        BigDecimal sportCap = min(sportActivity, BigDecimal.valueOf(cfg.getCapSport()));
        BigDecimal artRaw = min(art, BigDecimal.valueOf(cfg.getCapArt()));
        BigDecimal laborRaw = min(labor, BigDecimal.valueOf(cfg.getCapLabor()));

        BigDecimal intelRaw = courseAvg.multiply(new BigDecimal("0.85")).add(intelCap.multiply(new BigDecimal("0.15")));
        BigDecimal sportRaw = sportCourseAvg.multiply(new BigDecimal("0.85")).add(sportCap.multiply(new BigDecimal("0.15")));

        BigDecimal total = moralRaw.multiply(BigDecimal.valueOf(cfg.getwMoral()))
                .add(intelRaw.multiply(BigDecimal.valueOf(cfg.getwIntel())))
                .add(sportRaw.multiply(BigDecimal.valueOf(cfg.getwSport())))
                .add(artRaw.multiply(BigDecimal.valueOf(cfg.getwArt())))
                .add(laborRaw.multiply(BigDecimal.valueOf(cfg.getwLabor())));

        ScoreResult result = new ScoreResult();
        result.setCourseAvg(courseAvg.setScale(2, RoundingMode.HALF_UP));
        result.setIntelInnovation(intelInnovation.setScale(2, RoundingMode.HALF_UP));
        result.setSportActivity(sportActivity.setScale(2, RoundingMode.HALF_UP));
        result.setMoralRaw(moralRaw.setScale(2, RoundingMode.HALF_UP));
        result.setIntelRaw(intelRaw.setScale(2, RoundingMode.HALF_UP));
        result.setSportRaw(sportRaw.setScale(2, RoundingMode.HALF_UP));
        result.setArtRaw(artRaw.setScale(2, RoundingMode.HALF_UP));
        result.setLaborRaw(laborRaw.setScale(2, RoundingMode.HALF_UP));
        result.setTotalScore(total.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) > 0 ? b : a;
    }

    private ScoringConfigEntity defaultConfig() {
        ScoringConfigEntity cfg = new ScoringConfigEntity();
        cfg.setwMoral(0.15);
        cfg.setwIntel(0.60);
        cfg.setwSport(0.10);
        cfg.setwArt(0.075);
        cfg.setwLabor(0.075);
        cfg.setCapMoral(100.0);
        cfg.setCapIntel(100.0);
        cfg.setCapSport(100.0);
        cfg.setCapArt(100.0);
        cfg.setCapLabor(100.0);
        cfg.setScoreModel("STRICT_FORMULA");
        cfg.setPrecedenceMode("COLLEGE_FIRST");
        cfg.setAppealDays(10);
        return cfg;
    }
}
