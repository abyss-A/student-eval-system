package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class ReviewDecisionRequest {

    @NotBlank(message = "操作类型不能为空")
    private String action;

    private BigDecimal adjustedScore;

    private String reason;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BigDecimal getAdjustedScore() {
        return adjustedScore;
    }

    public void setAdjustedScore(BigDecimal adjustedScore) {
        this.adjustedScore = adjustedScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
