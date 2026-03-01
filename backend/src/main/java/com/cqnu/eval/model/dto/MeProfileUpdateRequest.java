package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class MeProfileUpdateRequest {

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^[0-9\\-]{7,20}$", message = "联系电话格式不正确")
    private String phone;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
