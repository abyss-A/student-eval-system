package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class RegisterStudentRequest {

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 32, message = "Password length must be 6-32")
    private String password;

    @NotBlank(message = "Real name cannot be blank")
    @Size(max = 32, message = "Real name length cannot exceed 32")
    private String realName;

    @NotBlank(message = "Gender cannot be blank")
    private String gender;

    @NotBlank(message = "Account no cannot be blank")
    @Size(max = 32, message = "Account no length cannot exceed 32")
    private String accountNo;

    @NotBlank(message = "Grade class cannot be blank")
    @Size(max = 64, message = "Grade class length cannot exceed 64")
    private String gradeClass;

    @NotBlank(message = "Phone cannot be blank")
    @Pattern(regexp = "^[0-9\\-]{7,20}$", message = "Phone format is invalid")
    private String phone;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getGradeClass() {
        return gradeClass;
    }

    public void setGradeClass(String gradeClass) {
        this.gradeClass = gradeClass;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

}
