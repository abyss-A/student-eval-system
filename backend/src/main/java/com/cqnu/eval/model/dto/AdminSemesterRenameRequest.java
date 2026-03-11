package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AdminSemesterRenameRequest {

    @NotBlank(message = "学期名称不能为空")
    @Size(max = 64, message = "学期名称不能超过64位")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

