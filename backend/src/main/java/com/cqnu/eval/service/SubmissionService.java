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
        SubmissionEntity submission = checkSubmissionEditableByStudent(submissionId, studentId);
        boolean rolledBackToDraft = rollbackToDraftIfSubmitted(submission);

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
        if (rolledBackToDraft) {
            resetReviewStatusForAllItems(submissionId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveActivities(Long submissionId, Long studentId, BatchActivityRequest request) {
        SubmissionEntity submission = checkSubmissionEditableByStudent(submissionId, studentId);
        boolean rolledBackToDraft = rollbackToDraftIfSubmitted(submission);

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
        if (rolledBackToDraft) {
            resetReviewStatusForAllItems(submissionId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveActivitiesByModule(Long submissionId, Long studentId, String moduleType, BatchActivityRequest request) {
        SubmissionEntity submission = checkSubmissionEditableByStudent(submissionId, studentId);
        boolean rolledBackToDraft = rollbackToDraftIfSubmitted(submission);

        String module = normalizeModuleType(moduleType);
        if (module.isBlank()) {
            throw new BizException(40001, "moduleType cannot be blank");
        }
        if (!isAllowedModule(module)) {
            throw new BizException(40001, "濠电姷鏁搁崑鐐哄垂閸洖绠伴柛婵勫劤閻捇鏌ｉ姀銏╃劸闁哄鐒﹂妵鍕即濡も偓娴滄儳螖閻橀潧浠﹂柛鏃€鐗犻獮蹇涘川鐎涙ê鈧粯淇婇姘倯婵炲牆鎲＄换婵堝枈濡椿娼戦梺鍓茬厛娴滎亪宕洪埀顒併亜閹烘垵鏆為柣婵愪邯閺屾稓鈧絻鍔岄崝銈囩磼鏉堛劌娴い銏℃礋婵＄兘顢欓崗纰卞敳闂? " + moduleType);
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
        if (rolledBackToDraft) {
            resetReviewStatusForAllItems(submissionId);
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
                throw new BizException(40001, "闂傚倸鍊搁崐鎼佸磹閸濄儮鍋撳鐓庡闁逞屽墯閸戣绂嶅鍫濈厺閹兼番鍔婇埀顑跨窔閹灝顓兼径瀣ф嫼闂佸憡绻傜€氼喛鍊撮梻浣告啞椤棝宕ㄩ鍛稐闂備焦鏋奸弲娑㈠疮娴兼潙鐓濋柡鍐ㄥ€甸崑鎾荤嵁閸喖濮庡┑鈽嗗亽閸欏啫鐣峰┑瀣劦妞ゆ帒鍊荤壕? " + trimmed);
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
                throw new BizException(40401, "闂傚倸鍊搁崐鎼佸磹閸濄儮鍋撳鐓庡闁逞屽墯閸戣绂嶅鍫濈厺閹兼番鍔岄～鍛存煥濞戞ê顏ら柡瀣墵閹鐛崹顔煎闂佺娅曢崝娆撶嵁濡ゅ懏鍋愮紓浣诡焽閸? " + id);
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
        SubmissionEntity entity = checkSubmissionEditableByStudent(submissionId, studentId);
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
        if (!"COUNSELOR_REVIEWED".equalsIgnoreCase(entity.getStatus())) {
            throw new BizException(40003, "Only counselor-reviewed submissions can be finalized");
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

        ScoreResult previewResult = calculateScore(submissionId, false);
        ScoreResult reviewedResult = calculateScore(submissionId, true);
        boolean reviewReady = isReviewReady(entity);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("submissionId", submissionId);
        map.put("status", entity.getStatus());
        map.put("reviewReady", reviewReady);

        putScore(map, "preview", previewResult);
        putScore(map, "reviewed", reviewedResult);

        // Backward-compatible fields: keep old keys available as preview scores.
        map.put("moralRaw", previewResult.getMoralRaw());
        map.put("intelRaw", previewResult.getIntelRaw());
        map.put("sportRaw", previewResult.getSportRaw());
        map.put("artRaw", previewResult.getArtRaw());
        map.put("laborRaw", previewResult.getLaborRaw());
        map.put("moralScore", previewResult.getMoralScore());
        map.put("intelScore", previewResult.getIntelScore());
        map.put("sportScore", previewResult.getSportScore());
        map.put("artScore", previewResult.getArtScore());
        map.put("laborScore", previewResult.getLaborScore());
        map.put("totalScore", previewResult.getTotalScore());
        map.put("courseAvg", previewResult.getCourseAvg());
        map.put("intelCourseAvg", previewResult.getIntelCourseAvg());
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

    public List<Map<String, Object>> listCounselorReviewedTasks() {
        return submissionMapper.listCounselorReviewedTasks();
    }

    private SubmissionEntity checkSubmissionEditableByStudent(Long submissionId, Long studentId) {
        SubmissionEntity entity = submissionMapper.findById(submissionId);
        if (entity == null) {
            throw new BizException(40401, "Submission not found");
        }
        if (submissionMapper.checkOwner(submissionId, studentId) == 0) {
            throw new BizException(40301, "No permission to operate this submission");
        }
        String status = entity.getStatus() == null ? "" : entity.getStatus().trim().toUpperCase(Locale.ROOT);
        if ("COUNSELOR_REVIEWED".equals(status) || "FINALIZED".equals(status) || "PUBLISHED".equals(status)) {
            throw new BizException(40003, "Current submission status is not editable");
        }
        return entity;
    }

    private boolean rollbackToDraftIfSubmitted(SubmissionEntity entity) {
        String status = entity.getStatus() == null ? "" : entity.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!"SUBMITTED".equals(status)) {
            return false;
        }
        entity.setStatus("DRAFT");
        entity.setSubmittedAt(null);
        submissionMapper.updateScoresAndStatus(entity);
        return true;
    }

    private void resetReviewStatusForAllItems(Long submissionId) {
        courseItemMapper.resetReviewBySubmissionId(submissionId);
        activityItemMapper.resetReviewBySubmissionId(submissionId);
    }

    private boolean isReviewReady(SubmissionEntity submission) {
        if (submission == null) {
            return false;
        }
        String status = submission.getStatus() == null ? "" : submission.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!"SUBMITTED".equals(status)
                && !"COUNSELOR_REVIEWED".equals(status)
                && !"FINALIZED".equals(status)
                && !"PUBLISHED".equals(status)) {
            return false;
        }
        int total = courseItemMapper.countBySubmissionId(submission.getId())
                + activityItemMapper.countBySubmissionId(submission.getId());
        int reviewed = courseItemMapper.countReviewedBySubmissionId(submission.getId())
                + activityItemMapper.countReviewedBySubmissionId(submission.getId());
        return reviewed >= total;
    }

    private void putScore(Map<String, Object> map, String prefix, ScoreResult result) {
        if (map == null || result == null) {
            return;
        }
        map.put(prefix + "MoralRaw", result.getMoralRaw());
        map.put(prefix + "IntelRaw", result.getIntelRaw());
        map.put(prefix + "SportRaw", result.getSportRaw());
        map.put(prefix + "ArtRaw", result.getArtRaw());
        map.put(prefix + "LaborRaw", result.getLaborRaw());
        map.put(prefix + "MoralScore", result.getMoralScore());
        map.put(prefix + "IntelScore", result.getIntelScore());
        map.put(prefix + "SportScore", result.getSportScore());
        map.put(prefix + "ArtScore", result.getArtScore());
        map.put(prefix + "LaborScore", result.getLaborScore());
        map.put(prefix + "TotalScore", result.getTotalScore());
        map.put(prefix + "CourseAvg", result.getCourseAvg());
        map.put(prefix + "IntelCourseAvg", result.getIntelCourseAvg());
    }

    public ScoreResult calculateScore(Long submissionId) {
        return calculateScore(submissionId, true);
    }

    public ScoreResult calculateScore(Long submissionId, boolean useReviewedScore) {
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
            BigDecimal score = useReviewedScore
                    ? safe(course.getReviewerScore() == null ? course.getScore() : course.getReviewerScore())
                    : safe(course.getScore());
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

        // 闂備礁鎼幊妯肩磽濮樿泛纾绘繛鎴炵鐎氭岸鏌涢幘妞诲亾闁哄被鍊濋弻娑滎槻婵炲眰鍊濆畷瑙勬償閵婏附娅栧┑顔斤供閸嬪棛绮旀總鍛婄厾闁艰壈娉涢拕鍏笺亜閺囩喎鈻曟慨濠佺矙婵″爼宕担鍏夋瀻闂備焦瀵х粙鎴炵附閺冨倻绠旈柡宥冨妿椤╃兘鏌涘☉鍗炵仯闁糕晝濞€閹鎷呭ù瀣壕鐎规洖娲ㄩ悾杈ㄧ箾?闂傚倷鐒﹁ぐ鍐矓閹惰棄绠?闂備礁鎲￠崝鏇犵矓閹惰棄绠為柕濞炬櫅杩?
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
            BigDecimal score = useReviewedScore
                    ? safe(activity.getFinalScore() == null ? activity.getSelfScore() : activity.getFinalScore())
                    : safe(activity.getSelfScore());
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



