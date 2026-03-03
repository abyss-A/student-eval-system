package com.cqnu.eval.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SubmissionEntity {
    private Long id;
    private Long semesterId;
    private Long studentId;
    private String status;
    private BigDecimal moralRaw;
    private BigDecimal intelRaw;
    private BigDecimal sportRaw;
    private BigDecimal artRaw;
    private BigDecimal laborRaw;
    private BigDecimal totalScore;
    private LocalDateTime submittedAt;
    private LocalDateTime finalizedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime counselorReadyAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getMoralRaw() {
        return moralRaw;
    }

    public void setMoralRaw(BigDecimal moralRaw) {
        this.moralRaw = moralRaw;
    }

    public BigDecimal getIntelRaw() {
        return intelRaw;
    }

    public void setIntelRaw(BigDecimal intelRaw) {
        this.intelRaw = intelRaw;
    }

    public BigDecimal getSportRaw() {
        return sportRaw;
    }

    public void setSportRaw(BigDecimal sportRaw) {
        this.sportRaw = sportRaw;
    }

    public BigDecimal getArtRaw() {
        return artRaw;
    }

    public void setArtRaw(BigDecimal artRaw) {
        this.artRaw = artRaw;
    }

    public BigDecimal getLaborRaw() {
        return laborRaw;
    }

    public void setLaborRaw(BigDecimal laborRaw) {
        this.laborRaw = laborRaw;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCounselorReadyAt() {
        return counselorReadyAt;
    }

    public void setCounselorReadyAt(LocalDateTime counselorReadyAt) {
        this.counselorReadyAt = counselorReadyAt;
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
