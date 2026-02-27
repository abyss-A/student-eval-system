package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotBlank;

public class FeedbackHandleRequest {

    @NotBlank(message = "action不能为空")
    private String action;

    private String replyContent;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReplyContent() {
        return replyContent;
    }

    public void setReplyContent(String replyContent) {
        this.replyContent = replyContent;
    }
}

