package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.ActivityItemMapper;
import com.cqnu.eval.mapper.CourseItemMapper;
import com.cqnu.eval.mapper.ReviewLogMapper;
import com.cqnu.eval.mapper.SubmissionMapper;
import com.cqnu.eval.model.dto.ReviewDecisionRequest;
import com.cqnu.eval.model.entity.ActivityItemEntity;
import com.cqnu.eval.model.entity.CourseItemEntity;
import com.cqnu.eval.model.entity.ReviewLogEntity;
import com.cqnu.eval.model.entity.SubmissionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReviewService {

    private final SubmissionMapper submissionMapper;
    private final CourseItemMapper courseItemMapper;
    private final ActivityItemMapper activityItemMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final SubmissionService submissionService;

    public ReviewService(SubmissionMapper submissionMapper,
                         CourseItemMapper courseItemMapper,
                         ActivityItemMapper activityItemMapper,
                         ReviewLogMapper reviewLogMapper,
                         SubmissionService submissionService) {
        this.submissionMapper = submissionMapper;
        this.courseItemMapper = courseItemMapper;
        this.activityItemMapper = activityItemMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.submissionService = submissionService;
    }

    public List<Map<String, Object>> listTasks() {
        return submissionMapper.listSubmittedTasks();
    }

    @Transactional(rollbackFor = Exception.class)
    public void decision(String itemType, Long itemId, ReviewDecisionRequest request, Long reviewerId) {
        String action = normalizeAction(request.getAction());
        if (!isSupportedAction(action)) {
            throw new BizException(40001, "Unsupported review action");
        }

        if ("COURSE".equalsIgnoreCase(itemType)) {
            if (isDeleteReviewAction(action)) {
                handleCourseDeleteReview(itemId, action, request, reviewerId);
            } else {
                handleCourse(itemId, action, request, reviewerId);
            }
            return;
        }
        if ("ACTIVITY".equalsIgnoreCase(itemType)) {
            if (isDeleteReviewAction(action)) {
                handleActivityDeleteReview(itemId, action, request, reviewerId);
            } else {
                handleActivity(itemId, action, request, reviewerId);
            }
            return;
        }
        throw new BizException(40001, "Unsupported item type");
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitToAdmin(Long submissionId) {
        SubmissionEntity submission = submissionMapper.findById(submissionId);
        if (submission == null) {
            throw new BizException(40401, "Submission not found");
        }
        if (!"SUBMITTED".equalsIgnoreCase(submission.getStatus())) {
            throw new BizException(40003, "Current submission status cannot be submitted to admin");
        }

        SubmissionService.ReviewStats stats = submissionService.getReviewStats(submissionId);
        if (stats.getReviewPendingCount() > 0) {
            throw new BizException(40001, "Submission still has unreviewed items");
        }
        if (stats.getReviewRejectedCount() > 0) {
            throw new BizException(40001, "Submission still has rejected items");
        }

        submissionService.recalculate(submissionId);
        int updated = submissionMapper.updateStatusIfCurrent(submissionId, "SUBMITTED", "COUNSELOR_REVIEWED");
        if (updated == 0) {
            throw new BizException(40901, "Status changed, please refresh and retry");
        }
    }

    private void handleCourse(Long itemId, String action, ReviewDecisionRequest req, Long reviewerId) {
        CourseItemEntity item = courseItemMapper.findById(itemId);
        if (item == null) {
            throw new BizException(40401, "Course item not found");
        }
        ensureSubmissionSubmitted(item.getSubmissionId());

        BigDecimal before = item.getReviewerScore() == null ? item.getScore() : item.getReviewerScore();
        BigDecimal after;
        String reason = normalizeReason(req.getReason());
        int updated;

        if ("APPROVE".equals(action)) {
            after = safe(item.getScore());
            updated = courseItemMapper.approveIfPending(itemId, reason);
        } else if ("REJECT".equals(action)) {
            after = BigDecimal.ZERO;
            updated = courseItemMapper.rejectIfPending(itemId, reason);
        } else {
            after = safe(item.getScore());
            reason = null;
            updated = courseItemMapper.undoIfReviewed(itemId);
        }

        if (updated == 0) {
            throw new BizException(40901, "Status changed, please refresh and retry");
        }
        ensureSubmissionSubmitted(item.getSubmissionId());

        log("COURSE", itemId, item.getSubmissionId(), action, before, after, reason, reviewerId);
        submissionService.recalculate(item.getSubmissionId());
        refreshCounselorReadyAt(item.getSubmissionId());
    }

    private void handleActivity(Long itemId, String action, ReviewDecisionRequest req, Long reviewerId) {
        ActivityItemEntity item = activityItemMapper.findById(itemId);
        if (item == null) {
            throw new BizException(40401, "Activity item not found");
        }
        ensureSubmissionSubmitted(item.getSubmissionId());

        BigDecimal before = item.getFinalScore() == null ? item.getSelfScore() : item.getFinalScore();
        BigDecimal after;
        String reason = normalizeReason(req.getReason());
        int updated;

        if ("APPROVE".equals(action)) {
            after = safe(item.getSelfScore());
            updated = activityItemMapper.approveIfPending(itemId, reason);
        } else if ("REJECT".equals(action)) {
            after = BigDecimal.ZERO;
            updated = activityItemMapper.rejectIfPending(itemId, reason);
        } else {
            after = safe(item.getSelfScore());
            reason = null;
            updated = activityItemMapper.undoIfReviewed(itemId);
        }

        if (updated == 0) {
            throw new BizException(40901, "Status changed, please refresh and retry");
        }
        ensureSubmissionSubmitted(item.getSubmissionId());

        log("ACTIVITY", itemId, item.getSubmissionId(), action, before, after, reason, reviewerId);
        submissionService.recalculate(item.getSubmissionId());
        refreshCounselorReadyAt(item.getSubmissionId());
    }

    private void handleCourseDeleteReview(Long itemId, String action, ReviewDecisionRequest req, Long reviewerId) {
        CourseItemEntity item = courseItemMapper.findById(itemId);
        if (item == null) {
            throw new BizException(40401, "Course item not found");
        }
        ensureSubmissionSubmitted(item.getSubmissionId());

        BigDecimal before = item.getReviewerScore() == null ? item.getScore() : item.getReviewerScore();
        BigDecimal after = before;
        String reason = normalizeReason(req.getReason());
        int updated;

        if ("APPROVE_DELETE".equals(action)) {
            after = BigDecimal.ZERO;
            updated = courseItemMapper.approveDeleteIfRequested(itemId);
        } else if ("REJECT_DELETE".equals(action)) {
            after = safe(item.getReviewerScore());
            updated = courseItemMapper.rejectDeleteIfRequested(itemId);
        } else {
            after = safe(item.getReviewerScore());
            reason = null;
            updated = courseItemMapper.undoDeletedToRejected(itemId);
        }

        if (updated == 0) {
            throw new BizException(40901, "Status changed, please refresh and retry");
        }
        ensureSubmissionSubmitted(item.getSubmissionId());

        log("COURSE", itemId, item.getSubmissionId(), action, before, after, reason, reviewerId);
        submissionService.recalculate(item.getSubmissionId());
        refreshCounselorReadyAt(item.getSubmissionId());
    }

    private void handleActivityDeleteReview(Long itemId, String action, ReviewDecisionRequest req, Long reviewerId) {
        ActivityItemEntity item = activityItemMapper.findById(itemId);
        if (item == null) {
            throw new BizException(40401, "Activity item not found");
        }
        ensureSubmissionSubmitted(item.getSubmissionId());

        BigDecimal before = item.getFinalScore() == null ? item.getSelfScore() : item.getFinalScore();
        BigDecimal after = before;
        String reason = normalizeReason(req.getReason());
        int updated;

        if ("APPROVE_DELETE".equals(action)) {
            after = BigDecimal.ZERO;
            updated = activityItemMapper.approveDeleteIfRequested(itemId);
        } else if ("REJECT_DELETE".equals(action)) {
            after = safe(item.getFinalScore());
            updated = activityItemMapper.rejectDeleteIfRequested(itemId);
        } else {
            after = safe(item.getFinalScore());
            reason = null;
            updated = activityItemMapper.undoDeletedToRejected(itemId);
        }

        if (updated == 0) {
            throw new BizException(40901, "Status changed, please refresh and retry");
        }
        ensureSubmissionSubmitted(item.getSubmissionId());

        log("ACTIVITY", itemId, item.getSubmissionId(), action, before, after, reason, reviewerId);
        submissionService.recalculate(item.getSubmissionId());
        refreshCounselorReadyAt(item.getSubmissionId());
    }

    private void ensureSubmissionSubmitted(Long submissionId) {
        SubmissionEntity submission = submissionMapper.findById(submissionId);
        if (submission == null) {
            throw new BizException(40401, "Submission not found");
        }
        if (!"SUBMITTED".equalsIgnoreCase(submission.getStatus())) {
            throw new BizException(40003, "Current submission is not in reviewable status");
        }
    }

    private void refreshCounselorReadyAt(Long submissionId) {
        SubmissionService.ReviewStats stats = submissionService.getReviewStats(submissionId);
        boolean readyToSubmit = stats.getReviewPendingCount() == 0 && stats.getReviewRejectedCount() == 0;
        submissionMapper.updateCounselorReadyAt(submissionId, readyToSubmit ? LocalDateTime.now() : null);
    }

    private boolean isDeleteReviewAction(String action) {
        return "APPROVE_DELETE".equals(action)
                || "REJECT_DELETE".equals(action)
                || "UNDO_DELETE".equals(action);
    }

    private boolean isSupportedAction(String action) {
        return "APPROVE".equals(action)
                || "REJECT".equals(action)
                || "UNDO".equals(action)
                || "APPROVE_DELETE".equals(action)
                || "REJECT_DELETE".equals(action)
                || "UNDO_DELETE".equals(action);
    }

    private String normalizeAction(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeReason(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void log(String itemType,
                     Long itemId,
                     Long submissionId,
                     String action,
                     BigDecimal before,
                     BigDecimal after,
                     String reason,
                     Long reviewerId) {
        ReviewLogEntity log = new ReviewLogEntity();
        log.setItemType(itemType);
        log.setItemId(itemId);
        log.setSubmissionId(submissionId);
        log.setAction(action);
        log.setScoreBefore(before);
        log.setScoreAfter(after);
        log.setReason(reason);
        log.setReviewerId(reviewerId);
        reviewLogMapper.insert(log);
    }
}
