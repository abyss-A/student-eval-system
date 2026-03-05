package com.cqnu.eval.model.dto;

public class RegisterStudentResponse {
    private Long userId;
    private String accountNo;
    private String realName;

    public RegisterStudentResponse() {
    }

    public RegisterStudentResponse(Long userId, String accountNo, String realName) {
        this.userId = userId;
        this.accountNo = accountNo;
        this.realName = realName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}
