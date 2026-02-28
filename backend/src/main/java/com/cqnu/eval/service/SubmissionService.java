package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.ActivityItemMapper;
import com.cqnu.eval.mapper.AttachmentMapper;
import com.cqnu.eval.mapper.CourseItemMapper;
import com.cqnu.eval.mapper.ScoringConfigMapper;
import com.cqnu.eval.mapper.SemesterMapper;
import com.cqnu.eval.mapper.SubmissionMapper;
import com.cqnu.eval.mapper.UserMapper;
import com.cqnu.eval.model.dto.ActivityItemInput;
import com.cqnu.eval.model.dto.BatchActivityRequest;
import com.cqnu.eval.model.dto.BatchCourseRequest;
import com.cqnu.eval.model.dto.CourseItemInput;
import com.cqnu.eval.model.entity.ActivityItemEntity;
import com.cqnu.eval.model.entity.AttachmentEntity;
import com.cqnu.eval.model.entity.CourseItemEntity;
import com.cqnu.eval.model.entity.ScoringConfigEntity;
import com.cqnu.eval.model.entity.SemesterEntity;
import com.cqnu.eval.model.entity.SubmissionEntity;
import com.cqnu.eval.model.entity.UserEntity;
import com.cqnu.eval.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SubmissionService {

    private final SemesterMapper semesterMapper;
    private final SubmissionMapper submissionMapper;
    private final CourseItemMapper courseItemMapper;
    private final ActivityItemMapper activityItemMapper;
    private final UserMapper userMapper;
    private final ScoringConfigMapper scoringConfigMapper;
    private final AttachmentMapper attachmentMapper;

    public SubmissionService(SemesterMapper semesterMapper,
                             SubmissionMapper submissionMapper,
                             CourseItemMapper courseItemMapper,
                             ActivityItemMapper activityItemMapper,
                             UserMapper userMapper,
                             ScoringConfigMapper scoringConfigMapper,
                             AttachmentMapper attachmentMapper) {
        this.semesterMapper = semesterMapper;
        this.submissionMapper = submissionMapper;
        this.courseItemMapper = courseItemMapper;
        this.activityItemMapper = activityItemMapper;
        this.userMapper = userMapper;
        this.scoringConfigMapper = scoringConfigMapper;
        this.attachmentMapper = attachmentMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmissionEntity createOrGetMySubmission(Long studentId) {
        SemesterEntity active = semesterMapper.findActive();
        if (active == null) {
            throw new BizException(40401, "No active semester is configured, please contact admin");
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
            throw new BizException(40401, "Submission not found");
        }
        if ("STUDENT".equalsIgnoreCase(currentUser.getRole())
                && submissionMapper.checkOwner(submissionId, currentUser.getId()) == 0) {
            throw new BizException(40301, "No permission to access this submission");
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
            entity.setEvidenceFileIds(sanitizeEvidenceFileIds(item.getEvidenceFileIds(), studentId));
            entity.setReviewStatus("PENDING");
            entity.setReviewerComment(null);
            activityItemMapper.insert(entity);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveActivitiesByModule(Long submissionId, Long studentId, String moduleType, BatchActivityRequest request) {
        checkSubmissionEditableByStudent(submissionId, studentId);

        String module = normalizeModuleType(moduleType);
        if (module.isBlank()) {
            throw new BizException(40001, "moduleType cannot be blank");
        }
        if (!isAllowedModule(module)) {
            throw new BizException(40001, "婵犵數鍋為崹鍫曞箰閸濄儳鐭撻柣銏㈩焾閺嬩焦銇勯弴妤€浜惧Δ鐘靛仦閸旀牠骞忛崨瀛樺€绘俊顖氬悑濞堝憡绻濈喊妯活潑闁割煈浜崺鈧い鎺戝暞閻濐亪鏌涚€ｃ劌鍔ょ紒杈ㄥ浮椤㈡洟濡烽鍏碱唲闂? " + moduleType);
        }

        activityItemMapper.deleteBySubmissionIdAndModule(submissionId, module);
        for (ActivityItemInput item : request.getItems()) {
            ActivityItemEntity entity = new ActivityItemEntity();
            entity.setSubmissionId(submissionId);
            entity.setModuleType(module);
            entity.setTitle(item.getTitle());
            entity.setDescription(item.getDescription());
            entity.setSelfScore(item.getSelfScore());
            entity.setFinalScore(item.getSelfScore());
            entity.setEvidenceFileIds(sanitizeEvidenceFileIds(item.getEvidenceFileIds(), studentId));
            entity.setReviewStatus("PENDING");
            entity.setReviewerComment(null);
            activityItemMapper.insert(entity);
        }
    }

    private String sanitizeEvidenceFileIds(String raw, Long studentId) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] parts = raw.split(",");
        List<Long> ids = new ArrayList<>();
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String trimmed = p.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            long id;
            try {
                id = Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                throw new BizException(40001, "闂傚倸鍊搁崐鍝モ偓姘煎墰閳ь剚鍑规禍婊堝煝鎼淬劊鈧線鎮垮澶嬧拺闁告繂瀚倴闂佸憡顭嗛崨顕呮綗闂佹枼鏅涢崯浼村煝閺冨倵鍋撻獮鍨姎婵☆偒鍙冨畷婵嬪焵椤掑倻纾? " + trimmed);
            }
            if (id <= 0) {
                throw new BizException(40001, "Attachment ID must be a positive integer");
            }
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }

        if (ids.size() > 6) {
            throw new BizException(40001, "At most 6 evidence images are allowed for one activity");
        }

        // Only allow referencing images uploaded by the current student.
        for (Long id : ids) {
            AttachmentEntity meta = attachmentMapper.findById(id);
            if (meta == null) {
                throw new BizException(40401, "闂傚倸鍊搁崐鍝モ偓姘煎墰閳ь剚鍑规禍婊堝煝鎼淬劌顫呴柨娑樺閺嬫牠鎮楅獮鍨姎闁硅櫕鍔欓獮妤呮偐缂佹鍘? " + id);
            }
            if (meta.getUploaderId() == null || !meta.getUploaderId().equals(studentId)) {
                throw new BizException(40301, "Only self-uploaded evidence images can be referenced");
            }
            // Evidence material: only allow JPG/PNG images to be referenced.
            String mime = meta.getMimeType();
            if (mime != null) {
                mime = mime.trim().toLowerCase(Locale.ROOT);
                int semi = mime.indexOf(';');
                if (semi > 0) {
                    mime = mime.substring(0, semi).trim();
                }
            }
            boolean allowedByMime = "image/jpeg".equals(mime) || "image/png".equals(mime);

            String fileName = meta.getFileName();
            String nameLower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
            boolean allowedByExt = nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg") || nameLower.endsWith(".png");

            if (!allowedByMime && !allowedByExt) {
                throw new BizException(40001, "Evidence supports JPG/PNG images only");
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(id);
        }
        return sb.toString();
    }

    private String normalizeModuleType(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isAllowedModule(String module) {
        return "MORAL".equals(module)
                || "INTEL_PRO_INNOV".equals(module)
                || "SPORT_ACTIVITY".equals(module)
                || "ART".equals(module)
                || "LABOR".equals(module);
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
            throw new BizException(40401, "Submission not found");
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
            throw new BizException(40401, "Submission not found");
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
            throw new BizException(40401, "Submission not found");
        }
        if ("STUDENT".equalsIgnoreCase(currentUser.getRole())
                && submissionMapper.checkOwner(submissionId, currentUser.getId()) == 0) {
            throw new BizException(40301, "No permission to operate this submission");
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
        map.put("moralScore", result.getMoralScore());
        map.put("intelScore", result.getIntelScore());
        map.put("sportScore", result.getSportScore());
        map.put("artScore", result.getArtScore());
        map.put("laborScore", result.getLaborScore());
        map.put("totalScore", result.getTotalScore());
        map.put("courseAvg", result.getCourseAvg());
        map.put("intelCourseAvg", result.getIntelCourseAvg());
        map.put("formula", "综合总分 = 德育原始分×15% + 智育原始分×60% + 体育原始分×10% + 美育原始分×7.5% + 劳育原始分×7.5%");
        map.put("intelFormula", "智育原始分 = 智育课程加权平均分×85% + min(智育活动总分,100)×15%；智育计入分 = 智育原始分×60%");
        return map;
    }

    public List<Map<String, Object>> getRanking(Long semesterId) {
        List<Map<String, Object>> all = submissionMapper.listForRanking(semesterId);

        Map<String, Integer> classCounter = new HashMap<>();
        Map<String, Integer> majorCounter = new HashMap<>();
        for (Map<String, Object> row : all) {
            String className = String.valueOf(row.get("class_name"));
            String majorName = String.valueOf(row.get("major_name"));
            int classRank = classCounter.getOrDefault(className, 0) + 1;
            int majorRank = majorCounter.getOrDefault(majorName, 0) + 1;
            classCounter.put(className, classRank);
            majorCounter.put(majorName, majorRank);
            row.put("rankClass", classRank);
            row.put("rankMajor", majorRank);
        }
        return all;
    }

    public List<Map<String, Object>> listSubmittedTasks() {
        return submissionMapper.listSubmittedTasks();
    }

    private void checkSubmissionEditableByStudent(Long submissionId, Long studentId) {
        SubmissionEntity entity = submissionMapper.findById(submissionId);
        if (entity == null) {
            throw new BizException(40401, "Submission not found");
        }
        if (submissionMapper.checkOwner(submissionId, studentId) == 0) {
            throw new BizException(40301, "No permission to operate this submission");
        }
        if ("FINALIZED".equals(entity.getStatus()) || "PUBLISHED".equals(entity.getStatus())) {
            throw new BizException(40003, "当前测评单状态不可编辑");
        }
    }

    public ScoreResult calculateScore(Long submissionId) {
        SubmissionEntity submission = submissionMapper.findById(submissionId);
        if (submission == null) {
            throw new BizException(40401, "Submission not found");
        }

        ScoringConfigEntity cfg = scoringConfigMapper.findBySemesterId(submission.getSemesterId());
        if (cfg == null) {
            cfg = defaultConfig();
        }

        List<CourseItemEntity> courses = courseItemMapper.listBySubmissionId(submissionId);
        List<ActivityItemEntity> activities = activityItemMapper.listBySubmissionId(submissionId);

        BigDecimal intelCreditSum = BigDecimal.ZERO;
        BigDecimal intelWeightedSum = BigDecimal.ZERO;
        BigDecimal sportCourseCreditSum = BigDecimal.ZERO;
        BigDecimal sportCourseWeightedSum = BigDecimal.ZERO;

        for (CourseItemEntity course : courses) {
            BigDecimal score = safe(course.getReviewerScore() == null ? course.getScore() : course.getReviewerScore());
            BigDecimal credit = safe(course.getCredit());

            if (isIntelAcademicCourse(course)) {
                intelCreditSum = intelCreditSum.add(credit);
                intelWeightedSum = intelWeightedSum.add(score.multiply(credit));
            }

            if (isSportCourse(course)) {
                sportCourseCreditSum = sportCourseCreditSum.add(credit);
                sportCourseWeightedSum = sportCourseWeightedSum.add(score.multiply(credit));
            }
        }

        // 闂佸搫鎳樼紓姘跺磻濞戞碍瀚氶柛鎾椻偓閺屻倝鏌涜濞层倝宕规惔銊︽櫖婵﹩鍋嗙粔濂告煠閼艰泛钄兼い鏇熷▕濮婁粙濡堕崪浣光枎闂佹寧绋戞總鏃傜箔閺嶃劎顩烽柛娑卞灣閸╃娀鎮规担娴嬪亾瀹曞洨鐣辨繛?闂備焦褰冪粔鎶藉箞?闂佸憡鍔曠粔鎶藉箞閵娾晛违?
        BigDecimal courseAvg = intelCreditSum.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : intelWeightedSum.divide(intelCreditSum, 4, RoundingMode.HALF_UP);

        // If there is no sport course, sport course average should be 0 instead of a fixed baseline.
        BigDecimal sportCourseAvg = sportCourseCreditSum.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : sportCourseWeightedSum.divide(sportCourseCreditSum, 4, RoundingMode.HALF_UP);

        BigDecimal moral = BigDecimal.ZERO;
        BigDecimal intelInnovation = BigDecimal.ZERO;
        BigDecimal sportActivity = BigDecimal.ZERO;
        BigDecimal art = BigDecimal.ZERO;
        BigDecimal labor = BigDecimal.ZERO;

        for (ActivityItemEntity activity : activities) {
            BigDecimal score = safe(activity.getFinalScore() == null ? activity.getSelfScore() : activity.getFinalScore());
            String module = activity.getModuleType() == null
                    ? ""
                    : activity.getModuleType().trim().toUpperCase(Locale.ROOT);

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

        BigDecimal intelRaw = courseAvg.multiply(new BigDecimal("0.85"))
                .add(intelCap.multiply(new BigDecimal("0.15")));
        BigDecimal sportRaw = sportCourseAvg.multiply(new BigDecimal("0.85"))
                .add(sportCap.multiply(new BigDecimal("0.15")));

        BigDecimal moralScore = moralRaw.multiply(BigDecimal.valueOf(cfg.getwMoral()));
        BigDecimal intelScore = intelRaw.multiply(BigDecimal.valueOf(cfg.getwIntel()));
        BigDecimal sportScore = sportRaw.multiply(BigDecimal.valueOf(cfg.getwSport()));
        BigDecimal artScore = artRaw.multiply(BigDecimal.valueOf(cfg.getwArt()));
        BigDecimal laborScore = laborRaw.multiply(BigDecimal.valueOf(cfg.getwLabor()));

        BigDecimal total = moralScore
                .add(intelScore)
                .add(sportScore)
                .add(artScore)
                .add(laborScore);

        ScoreResult result = new ScoreResult();
        result.setCourseAvg(courseAvg.setScale(2, RoundingMode.HALF_UP));
        result.setIntelCourseAvg(courseAvg.setScale(2, RoundingMode.HALF_UP));
        result.setIntelInnovation(intelInnovation.setScale(2, RoundingMode.HALF_UP));
        result.setSportActivity(sportActivity.setScale(2, RoundingMode.HALF_UP));
        result.setMoralRaw(moralRaw.setScale(2, RoundingMode.HALF_UP));
        result.setIntelRaw(intelRaw.setScale(2, RoundingMode.HALF_UP));
        result.setSportRaw(sportRaw.setScale(2, RoundingMode.HALF_UP));
        result.setArtRaw(artRaw.setScale(2, RoundingMode.HALF_UP));
        result.setLaborRaw(laborRaw.setScale(2, RoundingMode.HALF_UP));
        result.setMoralScore(moralScore.setScale(2, RoundingMode.HALF_UP));
        result.setIntelScore(intelScore.setScale(2, RoundingMode.HALF_UP));
        result.setSportScore(sportScore.setScale(2, RoundingMode.HALF_UP));
        result.setArtScore(artScore.setScale(2, RoundingMode.HALF_UP));
        result.setLaborScore(laborScore.setScale(2, RoundingMode.HALF_UP));
        result.setTotalScore(total.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    private boolean isIntelAcademicCourse(CourseItemEntity course) {
        if (course == null) {
            return false;
        }
        if (isSportCourse(course)) {
            return false;
        }
        String type = course.getCourseType() == null ? "" : course.getCourseType().trim().toUpperCase(Locale.ROOT);
        if (type.isEmpty()) {
            return true;
        }
        return "REQUIRED".equals(type)
                || "RETAKE".equals(type)
                || "RELEARN".equals(type)
                || type.contains("MANDATORY")
                || type.contains("\u5fc5\u4fee")
                || type.contains("\u91cd\u4fee")
                || type.contains("\u518d\u4fee");
    }

    private boolean isSportCourse(CourseItemEntity course) {
        if (course == null) {
            return false;
        }
        String name = course.getCourseName() == null ? "" : course.getCourseName().toUpperCase(Locale.ROOT);
        String type = course.getCourseType() == null ? "" : course.getCourseType().toUpperCase(Locale.ROOT);
        if (type.contains("SPORT") || type.contains("PE") || type.contains("PHYSICAL") || type.contains("\u4f53\u80b2")) {
            return true;
        }
        if (name.contains("SPORT") || name.contains("PE") || name.contains("PHYSICAL")) {
            return true;
        }
        String rawName = course.getCourseName() == null ? "" : course.getCourseName().replace(" ", "");
        return rawName.contains("\u4f53\u80b2") || rawName.contains("\u4f53\u6d4b") || rawName.contains("\u4f53\u80fd");
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

