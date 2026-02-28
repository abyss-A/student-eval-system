package com.cqnu.eval.model.dto;

public class RegisterStudentResponse {
    private Long userId;
    private String username;
    private String realName;

    public RegisterStudentResponse() {
    }

    public RegisterStudentResponse(Long userId, String username, String realName) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}
