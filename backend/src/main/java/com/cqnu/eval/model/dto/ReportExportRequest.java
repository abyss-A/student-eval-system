package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotBlank;

public class ReportExportRequest {

    @NotBlank(message = "导出格式不能为空")
    private String format;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
