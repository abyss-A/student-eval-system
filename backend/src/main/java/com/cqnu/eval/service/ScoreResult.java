package com.cqnu.eval.service;

import java.math.BigDecimal;

public class ScoreResult {
    private BigDecimal moralRaw;
    private BigDecimal intelRaw;
    private BigDecimal sportRaw;
    private BigDecimal artRaw;
    private BigDecimal laborRaw;
    private BigDecimal totalScore;
    private BigDecimal courseAvg;
    private BigDecimal intelInnovation;
    private BigDecimal sportActivity;

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

    public BigDecimal getCourseAvg() {
        return courseAvg;
    }

    public void setCourseAvg(BigDecimal courseAvg) {
        this.courseAvg = courseAvg;
    }

    public BigDecimal getIntelInnovation() {
        return intelInnovation;
    }

    public void setIntelInnovation(BigDecimal intelInnovation) {
        this.intelInnovation = intelInnovation;
    }

    public BigDecimal getSportActivity() {
        return sportActivity;
    }

    public void setSportActivity(BigDecimal sportActivity) {
        this.sportActivity = sportActivity;
    }
}
