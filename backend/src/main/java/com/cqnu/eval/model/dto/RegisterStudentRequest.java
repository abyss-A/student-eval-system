package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class RegisterStudentRequest {

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6-32位之间")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名长度不能超过32位")
    private String realName;

    @NotBlank(message = "性别不能为空")
    private String gender;

    @NotBlank(message = "学号不能为空")
    @Size(max = 32, message = "学号长度不能超过32位")
    private String studentNo;

    @NotBlank(message = "年级班级不能为空")
    @Size(max = 64, message = "年级班级长度不能超过64位")
    private String gradeClass;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^[0-9\\-]{7,20}$", message = "联系电话格式不正确")
    private String phone;

    @Size(max = 64, message = "专业长度不能超过64位")
    private String majorName;

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

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
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

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }
}
