package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotBlank;

public class AdminAccountImportCommitRequest {

    @NotBlank(message = "预校验令牌不能为空")
    private String previewToken;

    public String getPreviewToken() {
        return previewToken;
    }

    public void setPreviewToken(String previewToken) {
        this.previewToken = previewToken;
    }
}
