package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class AdminAccountUpdateRequest {

    @Size(min = 4, max = 32, message = "学号/工号长度需在4-32位之间")
    private String accountNo;

    private String role;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名长度不能超过32位")
    private String realName;

    @NotBlank(message = "性别不能为空")
    private String gender;

    @Pattern(regexp = "^$|^[0-9\\-]{7,20}$", message = "联系电话格式不正确")
    private String phone;

    @Size(max = 64, message = "班级长度不能超过64位")
    private String className;

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
