package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotBlank;

public class FeedbackCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    // 逗号分隔的附件ID串，最多6张图片
    private String screenshotFileIds;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getScreenshotFileIds() {
        return screenshotFileIds;
    }

    public void setScreenshotFileIds(String screenshotFileIds) {
        this.screenshotFileIds = screenshotFileIds;
    }
}

