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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class SubmissionService {

    private static final String SPORT_MODULE = "SPORT_ACTIVITY";
    private static final String UNIVERSITY_PE_TITLE = "大学体育";
    private static final BigDecimal MAX_INPUT_SCORE = new BigDecimal("100");

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
        String status = normalizeStatus(submission.getStatus());
        validateNoSportCourseItems(request.getItems());
        if (!"SUBMITTED".equals(status)) {
            replaceAllCourses(submissionId, request);
            return;
        }

        ReviewStats stats = getReviewStats(submissionId);
        if (!"DONE_NEED_STUDENT_FIX".equals(stats.getReviewPhase())) {
            throw new BizException(40003, "审核阶段不可修改课程");
        }
        reviseRejectedCoursesOnly(submissionId, request);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveActivities(Long submissionId, Long studentId, BatchActivityRequest request) {
        SubmissionEntity submission = checkSubmissionEditableByStudent(submissionId, studentId);
        String status = normalizeStatus(submission.getStatus());
        validateSportActivityRules(request.getItems(), null);
        if (!"SUBMITTED".equals(status)) {
            replaceAllActivities(submissionId, studentId, request);
            return;
        }

        ReviewStats stats = getReviewStats(submissionId);
        if (!"DONE_NEED_STUDENT_FIX".equals(stats.getReviewPhase())) {
            throw new BizException(40003, "审核阶段不可修改活动");
        }
        reviseRejectedActivitiesOnly(submissionId, studentId, request, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveActivitiesByModule(Long submissionId, Long studentId, String moduleType, BatchActivityRequest request) {
        SubmissionEntity submission = checkSubmissionEditableByStudent(submissionId, studentId);

        String module = normalizeModuleType(moduleType);
        if (module.isBlank()) {
            throw new BizException(40001, "moduleType cannot be blank");
        }
        if (!isAllowedModule(module)) {
            throw new BizException(40001, "Invalid moduleType: " + moduleType);
        }

        validateSportActivityRules(request.getItems(), module);

        String status = normalizeStatus(submission.getStatus());
        if (!"SUBMITTED".equals(status)) {
            replaceActivitiesByModule(submissionId, studentId, module, request);
            return;
        }

        ReviewStats stats = getReviewStats(submissionId);
        if (!"DONE_NEED_STUDENT_FIX".equals(stats.getReviewPhase())) {
            throw new BizException(40003, "审核阶段不可修改活动");
        }
        reviseRejectedActivitiesOnly(submissionId, studentId, request, module);
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
                throw new BizException(40001, "Attachment ID must be numeric: " + trimmed);
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
                throw new BizException(40401, "Attachment does not exist: " + id);
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

    private String normalizeStatus(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isUniversityPeTitle(String title) {
        return UNIVERSITY_PE_TITLE.equals(trim(title));
    }

    private void validateNoSportCourseItems(List<CourseItemInput> items) {
        if (items == null) {
            return;
        }
        for (CourseItemInput item : items) {
            if (item == null) {
                continue;
            }
            if (isSportCourse(item.getCourseName(), item.getCourseType())) {
                throw new BizException(40001, "课程成绩页不支持填写体育成绩，请到体育模块填写“大学体育”");
            }
        }
    }

    private void validateSportActivityRules(List<ActivityItemInput> items, String moduleScope) {
        if (items == null) {
            return;
        }

        int universityPeCount = 0;
        for (ActivityItemInput item : items) {
            if (item == null) {
                continue;
            }
            String module = moduleScope == null
                    ? normalizeModuleType(item.getModuleType())
                    : normalizeModuleType(moduleScope);
            if (!SPORT_MODULE.equals(module)) {
                continue;
            }
            BigDecimal score = item.getSelfScore();
            if (score != null && score.compareTo(MAX_INPUT_SCORE) > 0) {
                throw new BizException(40001, "体育模块分数必须在 0 到 100 之间");
            }
            if (isUniversityPeTitle(item.getTitle())) {
                universityPeCount++;
            }
        }

        if (universityPeCount > 1) {
            throw new BizException(40001, "体育模块中“大学体育”最多只能填写一条");
        }
    }

    private void replaceAllCourses(Long submissionId, BatchCourseRequest request) {
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

    private void replaceAllActivities(Long submissionId, Long studentId, BatchActivityRequest request) {
        activityItemMapper.deleteBySubmissionId(submissionId);
        for (ActivityItemInput item : request.getItems()) {
            String module = normalizeModuleType(item.getModuleType());
            if (module.isBlank() || !isAllowedModule(module)) {
                throw new BizException(40001, "Invalid moduleType: " + item.getModuleType());
            }
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

    private void replaceActivitiesByModule(Long submissionId, Long studentId, String module, BatchActivityRequest request) {
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

    private void reviseRejectedCoursesOnly(Long submissionId, BatchCourseRequest request) {
        List<CourseItemEntity> existing = courseItemMapper.listBySubmissionId(submissionId);
        Map<Long, CourseItemInput> incomingById = new HashMap<>();
        for (CourseItemInput input : request.getItems()) {
            Long id = input.getId();
            if (id == null || id <= 0) {
                throw new BizException(40001, "重提审核时必须携带原课程ID");
            }
            if (incomingById.put(id, input) != null) {
                throw new BizException(40001, "课程ID重复");
            }
        }

        Set<Long> existingIds = new HashSet<>();
        for (CourseItemEntity entity : existing) {
            existingIds.add(entity.getId());
        }
        if (incomingById.size() != existingIds.size() || !incomingById.keySet().equals(existingIds)) {
            throw new BizException(40001, "课程条目数量或ID不匹配，禁止增删条目");
        }

        for (CourseItemEntity current : existing) {
            CourseItemInput input = incomingById.get(current.getId());
            if (input == null) {
                throw new BizException(40001, "存在未知课程ID");
            }
            boolean changed = isCourseContentChanged(current, input);
            String reviewStatus = normalizeStatus(current.getReviewStatus());
            if (!"REJECTED".equals(reviewStatus)) {
                if (changed) {
                    throw new BizException(40001, "仅驳回课程允许修改，已通过/待审课程不可改");
                }
                continue;
            }
            if (!changed) {
                continue;
            }
            CourseItemEntity update = new CourseItemEntity();
            update.setId(current.getId());
            update.setSubmissionId(submissionId);
            update.setCourseName(input.getCourseName());
            update.setCourseType(input.getCourseType());
            update.setScore(input.getScore());
            update.setCredit(input.getCredit());
            update.setEvidenceFileId(input.getEvidenceFileId());
            update.setReviewerScore(current.getReviewerScore());
            int updated = courseItemMapper.updateEditableFields(update);
            if (updated == 0) {
                throw new BizException(40901, "状态已变化，请刷新后重试");
            }
        }
    }

    private void reviseRejectedActivitiesOnly(Long submissionId,
                                              Long studentId,
                                              BatchActivityRequest request,
                                              String moduleScope) {
        List<ActivityItemEntity> existing = moduleScope == null
                ? activityItemMapper.listBySubmissionId(submissionId)
                : activityItemMapper.listBySubmissionIdAndModule(submissionId, moduleScope);

        Map<Long, ActivityItemInput> incomingById = new HashMap<>();
        Map<Long, String> sanitizedEvidenceById = new HashMap<>();
        for (ActivityItemInput input : request.getItems()) {
            Long id = input.getId();
            if (id == null || id <= 0) {
                throw new BizException(40001, "重提审核时必须携带原活动ID");
            }
            if (incomingById.put(id, input) != null) {
                throw new BizException(40001, "活动ID重复");
            }
            String evidence = sanitizeEvidenceFileIds(input.getEvidenceFileIds(), studentId);
            sanitizedEvidenceById.put(id, evidence);
        }

        Set<Long> existingIds = new HashSet<>();
        for (ActivityItemEntity entity : existing) {
            existingIds.add(entity.getId());
        }
        if (incomingById.size() != existingIds.size() || !incomingById.keySet().equals(existingIds)) {
            throw new BizException(40001, "活动条目数量或ID不匹配，禁止增删条目");
        }

        for (ActivityItemEntity current : existing) {
            ActivityItemInput input = incomingById.get(current.getId());
            if (input == null) {
                throw new BizException(40001, "存在未知活动ID");
            }
            String evidence = sanitizedEvidenceById.getOrDefault(current.getId(), "");
            boolean changed = isActivityContentChanged(current, input, evidence);
            String reviewStatus = normalizeStatus(current.getReviewStatus());
            if (!"REJECTED".equals(reviewStatus)) {
                if (changed) {
                    throw new BizException(40001, "仅驳回活动允许修改，已通过/待审活动不可改");
                }
                continue;
            }
            if (!changed) {
                continue;
            }
            ActivityItemEntity update = new ActivityItemEntity();
            update.setId(current.getId());
            update.setSubmissionId(submissionId);
            update.setTitle(input.getTitle());
            update.setDescription(input.getDescription());
            update.setSelfScore(input.getSelfScore());
            update.setEvidenceFileIds(evidence);
            update.setFinalScore(current.getFinalScore());
            int updated = activityItemMapper.updateEditableFields(update);
            if (updated == 0) {
                throw new BizException(40901, "状态已变化，请刷新后重试");
            }
        }
    }

    private boolean isCourseContentChanged(CourseItemEntity current, CourseItemInput input) {
        if (!Objects.equals(trim(current.getCourseName()), trim(input.getCourseName()))) {
            return true;
        }
        if (!Objects.equals(normalizeStatus(current.getCourseType()), normalizeStatus(input.getCourseType()))) {
            return true;
        }
        if (!decimalEquals(current.getScore(), input.getScore())) {
            return true;
        }
        if (!decimalEquals(current.getCredit(), input.getCredit())) {
            return true;
        }
        return !Objects.equals(current.getEvidenceFileId(), input.getEvidenceFileId());
    }

    private boolean isActivityContentChanged(ActivityItemEntity current, ActivityItemInput input, String sanitizedEvidence) {
        if (!Objects.equals(trim(current.getTitle()), trim(input.getTitle()))) {
            return true;
        }
        if (!Objects.equals(trim(current.getDescription()), trim(input.getDescription()))) {
            return true;
        }
        if (!decimalEquals(current.getSelfScore(), input.getSelfScore())) {
            return true;
        }
        return !Objects.equals(trim(current.getEvidenceFileIds()), trim(sanitizedEvidence));
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmissionEntity submit(Long submissionId, Long studentId) {
        SubmissionEntity entity = checkSubmissionEditableByStudent(submissionId, studentId);
        String status = normalizeStatus(entity.getStatus());

        if ("DRAFT".equals(status)) {
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

        if (!"SUBMITTED".equals(status)) {
            throw new BizException(40003, "当前状态不可提交");
        }

        ReviewStats stats = getReviewStats(submissionId);
        if ("DONE_ALL_PASS".equals(stats.getReviewPhase())) {
            throw new BizException(40003, "已全部通过，无需再次提交");
        }
        if (!"DONE_NEED_STUDENT_FIX".equals(stats.getReviewPhase())) {
            throw new BizException(40003, "审核中，暂不可再次提交");
        }

        courseItemMapper.reopenRejectedBySubmissionId(submissionId);
        activityItemMapper.reopenRejectedBySubmissionId(submissionId);

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
        ReviewStats stats = getReviewStats(submissionId);
        boolean reviewReady = stats.getReviewPendingCount() == 0;
        boolean canStudentResubmit = "SUBMITTED".equals(normalizeStatus(entity.getStatus()))
                && "DONE_NEED_STUDENT_FIX".equals(stats.getReviewPhase());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("submissionId", submissionId);
        map.put("status", entity.getStatus());
        map.put("reviewReady", reviewReady);
        map.put("reviewPhase", stats.getReviewPhase());
        map.put("canStudentResubmit", canStudentResubmit);
        map.put("editableRejectedOnly", canStudentResubmit);
        map.put("reviewTotalCount", stats.getReviewTotalCount());
        map.put("reviewDoneCount", stats.getReviewDoneCount());
        map.put("reviewPendingCount", stats.getReviewPendingCount());
        map.put("reviewRejectedCount", stats.getReviewRejectedCount());
        map.put("reviewApprovedCount", stats.getReviewApprovedCount());
        map.put("rejectedItemIds", listRejectedItems(submissionId));

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
        map.put("formula", "综合总分 = 德育计入分 + 智育计入分 + 体育计入分 + 美育计入分 + 劳育计入分");
        map.put("intelFormula", "智育原始分 = 智育课程加权平均分 × 85% + min(智育活动总分, 100) × 15%；智育计入分 = 智育原始分 × 60%");
        map.put("sportFormula", "体育原始分 = 大学体育分 × 85% + min(体育活动总分, 100) × 15%；体育计入分 = 体育原始分 × 10%");
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

    public ReviewStats getReviewStats(Long submissionId) {
        int courseTotal = courseItemMapper.countBySubmissionId(submissionId);
        int activityTotal = activityItemMapper.countBySubmissionId(submissionId);
        int coursePending = courseItemMapper.countBySubmissionIdAndStatus(submissionId, "PENDING");
        int activityPending = activityItemMapper.countBySubmissionIdAndStatus(submissionId, "PENDING");
        int courseRejected = courseItemMapper.countBySubmissionIdAndStatus(submissionId, "REJECTED");
        int activityRejected = activityItemMapper.countBySubmissionIdAndStatus(submissionId, "REJECTED");
        int courseApproved = courseItemMapper.countBySubmissionIdAndStatus(submissionId, "APPROVED");
        int activityApproved = activityItemMapper.countBySubmissionIdAndStatus(submissionId, "APPROVED");

        int total = courseTotal + activityTotal;
        int pending = coursePending + activityPending;
        int rejected = courseRejected + activityRejected;
        int approved = courseApproved + activityApproved;
        int done = total - pending;

        String phase;
        if (total == 0) {
            phase = "DONE_ALL_PASS";
        } else if (pending == total) {
            phase = "NOT_REVIEWED";
        } else if (pending > 0) {
            phase = "IN_PROGRESS";
        } else if (rejected > 0) {
            phase = "DONE_NEED_STUDENT_FIX";
        } else {
            phase = "DONE_ALL_PASS";
        }

        return new ReviewStats(total, done, pending, rejected, approved, phase);
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

    private List<Map<String, Object>> listRejectedItems(Long submissionId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CourseItemEntity course : courseItemMapper.listBySubmissionId(submissionId)) {
            if (!"REJECTED".equals(normalizeStatus(course.getReviewStatus()))) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemType", "COURSE");
            row.put("itemId", course.getId());
            out.add(row);
        }
        for (ActivityItemEntity activity : activityItemMapper.listBySubmissionId(submissionId)) {
            if (!"REJECTED".equals(normalizeStatus(activity.getReviewStatus()))) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemType", "ACTIVITY");
            row.put("itemId", activity.getId());
            out.add(row);
        }
        return out;
    }

    private boolean decimalEquals(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    private String trim(String raw) {
        return raw == null ? "" : raw.trim();
    }

    public static class ReviewStats {
        private final int reviewTotalCount;
        private final int reviewDoneCount;
        private final int reviewPendingCount;
        private final int reviewRejectedCount;
        private final int reviewApprovedCount;
        private final String reviewPhase;

        public ReviewStats(int reviewTotalCount,
                           int reviewDoneCount,
                           int reviewPendingCount,
                           int reviewRejectedCount,
                           int reviewApprovedCount,
                           String reviewPhase) {
            this.reviewTotalCount = reviewTotalCount;
            this.reviewDoneCount = reviewDoneCount;
            this.reviewPendingCount = reviewPendingCount;
            this.reviewRejectedCount = reviewRejectedCount;
            this.reviewApprovedCount = reviewApprovedCount;
            this.reviewPhase = reviewPhase;
        }

        public int getReviewTotalCount() {
            return reviewTotalCount;
        }

        public int getReviewDoneCount() {
            return reviewDoneCount;
        }

        public int getReviewPendingCount() {
            return reviewPendingCount;
        }

        public int getReviewRejectedCount() {
            return reviewRejectedCount;
        }

        public int getReviewApprovedCount() {
            return reviewApprovedCount;
        }

        public String getReviewPhase() {
            return reviewPhase;
        }
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

        for (CourseItemEntity course : courses) {
            BigDecimal score = useReviewedScore
                    ? safe(course.getReviewerScore() == null ? course.getScore() : course.getReviewerScore())
                    : safe(course.getScore());
            BigDecimal credit = safe(course.getCredit());

            if (isIntelAcademicCourse(course)) {
                intelCreditSum = intelCreditSum.add(credit);
                intelWeightedSum = intelWeightedSum.add(score.multiply(credit));
            }

        }
        // New sport rule:
        // sportRaw = universityPeScore * 0.85 + min(sportActivityPool, capSport) * 0.15
        BigDecimal courseAvg = intelCreditSum.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : intelWeightedSum.divide(intelCreditSum, 4, RoundingMode.HALF_UP);

        BigDecimal moral = BigDecimal.ZERO;
        BigDecimal intelInnovation = BigDecimal.ZERO;
        BigDecimal sportActivityPool = BigDecimal.ZERO;
        BigDecimal universityPeScore = BigDecimal.ZERO;
        boolean universityPeFound = false;
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
                    if (isUniversityPeTitle(activity.getTitle()) && !universityPeFound) {
                        universityPeScore = score;
                        universityPeFound = true;
                    } else {
                        sportActivityPool = sportActivityPool.add(score);
                    }
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
        BigDecimal sportCap = min(sportActivityPool, BigDecimal.valueOf(cfg.getCapSport()));
        BigDecimal artRaw = min(art, BigDecimal.valueOf(cfg.getCapArt()));
        BigDecimal laborRaw = min(labor, BigDecimal.valueOf(cfg.getCapLabor()));

        BigDecimal intelRaw = courseAvg.multiply(new BigDecimal("0.85"))
                .add(intelCap.multiply(new BigDecimal("0.15")));
        BigDecimal sportRaw = universityPeScore.multiply(new BigDecimal("0.85"))
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
        result.setSportActivity(sportActivityPool.setScale(2, RoundingMode.HALF_UP));
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
        return isSportCourse(course.getCourseName(), course.getCourseType());
    }

    private boolean isSportCourse(String courseName, String courseType) {
        String name = courseName == null ? "" : courseName.toUpperCase(Locale.ROOT);
        String type = courseType == null ? "" : courseType.toUpperCase(Locale.ROOT);
        if (type.contains("SPORT") || type.contains("PE") || type.contains("PHYSICAL") || type.contains("\u4f53\u80b2")) {
            return true;
        }
        if (name.contains("SPORT") || name.contains("PE") || name.contains("PHYSICAL")) {
            return true;
        }
        String rawName = courseName == null ? "" : courseName.replace(" ", "");
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





