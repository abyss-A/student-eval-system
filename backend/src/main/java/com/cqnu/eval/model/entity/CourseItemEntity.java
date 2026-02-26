package com.cqnu.eval.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CourseItemEntity {
    private Long id;
    private Long submissionId;
    private String courseName;
    private String courseType;
    private BigDecimal score;
    private BigDecimal credit;
    private Long evidenceFileId;
    private String reviewStatus;
    private BigDecimal reviewerScore;
    private String reviewerComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public Long getEvidenceFileId() {
        return evidenceFileId;
    }

    public void setEvidenceFileId(Long evidenceFileId) {
        this.evidenceFileId = evidenceFileId;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public BigDecimal getReviewerScore() {
        return reviewerScore;
    }

    public void setReviewerScore(BigDecimal reviewerScore) {
        this.reviewerScore = reviewerScore;
    }

    public String getReviewerComment() {
        return reviewerComment;
    }

    public void setReviewerComment(String reviewerComment) {
        this.reviewerComment = reviewerComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
