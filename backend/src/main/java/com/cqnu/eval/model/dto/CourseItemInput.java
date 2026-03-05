package com.cqnu.eval.model.dto;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CourseItemInput {
    private Long id;

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    @NotBlank(message = "课程类型不能为空")
    private String courseType;

    @NotNull(message = "成绩不能为空")
    @DecimalMin(value = "0", message = "成绩不能小于0")
    @DecimalMax(value = "100", message = "成绩不能大于100")
    private BigDecimal score;

    @NotNull(message = "学分不能为空")
    @DecimalMin(value = "0", message = "学分不能小于0")
    private BigDecimal credit;

    private Long evidenceFileId;
    private Boolean deleteRequested;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getDeleteRequested() {
        return deleteRequested;
    }

    public void setDeleteRequested(Boolean deleteRequested) {
        this.deleteRequested = deleteRequested;
    }
}
