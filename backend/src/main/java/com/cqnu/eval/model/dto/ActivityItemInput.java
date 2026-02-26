package com.cqnu.eval.model.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ActivityItemInput {
    private Long id;

    @NotBlank(message = "板块不能为空")
    private String moduleType;

    @NotBlank(message = "活动标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "自评分不能为空")
    @DecimalMin(value = "0", message = "自评分不能小于0")
    private BigDecimal selfScore;

    private String evidenceFileIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getSelfScore() {
        return selfScore;
    }

    public void setSelfScore(BigDecimal selfScore) {
        this.selfScore = selfScore;
    }

    public String getEvidenceFileIds() {
        return evidenceFileIds;
    }

    public void setEvidenceFileIds(String evidenceFileIds) {
        this.evidenceFileIds = evidenceFileIds;
    }
}
