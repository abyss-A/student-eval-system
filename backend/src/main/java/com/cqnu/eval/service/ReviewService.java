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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        String action = request.getAction().toUpperCase(Locale.ROOT);
        if (!"APPROVE".equals(action) && !"REJECT".equals(action) && !"ADJUST".equals(action)) {
            throw new BizException(40001, "不支持的审核动作");
        }

        if ("COURSE".equalsIgnoreCase(itemType)) {
            handleCourse(itemId, action, request, reviewerId);
        } else if ("ACTIVITY".equalsIgnoreCase(itemType)) {
            handleActivity(itemId, action, request, reviewerId);
        } else {
            throw new BizException(40001, "不支持的条目类型");
        }
    }

    private void handleCourse(Long itemId, String action, ReviewDecisionRequest req, Long reviewerId) {
        CourseItemEntity item = courseItemMapper.findById(itemId);
        if (item == null) {
            throw new BizException(40401, "课程条目不存在");
        }
        BigDecimal before = item.getReviewerScore() == null ? item.getScore() : item.getReviewerScore();
        BigDecimal after = before;

        if ("REJECT".equals(action)) {
            after = BigDecimal.ZERO;
            item.setReviewStatus("REJECTED");
            item.setReviewerComment(req.getReason());
        } else if ("ADJUST".equals(action)) {
            if (req.getAdjustedScore() == null) {
                throw new BizException(40001, "改分时必须传adjustedScore");
            }
            after = req.getAdjustedScore();
            item.setReviewStatus("APPROVED");
            item.setReviewerComment(req.getReason());
        } else {
            item.setReviewStatus("APPROVED");
            item.setReviewerComment(req.getReason());
        }
        item.setReviewerScore(after);
        courseItemMapper.updateReview(item);

        log("COURSE", itemId, item.getSubmissionId(), action, before, after, req.getReason(), reviewerId);
        submissionService.recalculate(item.getSubmissionId());
    }

    private void handleActivity(Long itemId, String action, ReviewDecisionRequest req, Long reviewerId) {
        ActivityItemEntity item = activityItemMapper.findById(itemId);
        if (item == null) {
            throw new BizException(40401, "活动条目不存在");
        }
        BigDecimal before = item.getFinalScore() == null ? item.getSelfScore() : item.getFinalScore();
        BigDecimal after = before;

        if ("REJECT".equals(action)) {
            after = BigDecimal.ZERO;
            item.setReviewStatus("REJECTED");
            item.setReviewerComment(req.getReason());
        } else if ("ADJUST".equals(action)) {
            if (req.getAdjustedScore() == null) {
                throw new BizException(40001, "改分时必须传adjustedScore");
            }
            after = req.getAdjustedScore();
            item.setReviewStatus("APPROVED");
            item.setReviewerComment(req.getReason());
        } else {
            item.setReviewStatus("APPROVED");
            item.setReviewerComment(req.getReason());
        }
        item.setFinalScore(after);
        activityItemMapper.updateReview(item);

        log("ACTIVITY", itemId, item.getSubmissionId(), action, before, after, req.getReason(), reviewerId);
        submissionService.recalculate(item.getSubmissionId());
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
