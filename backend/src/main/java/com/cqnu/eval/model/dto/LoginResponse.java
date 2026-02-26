package com.cqnu.eval.model.dto;

public class LoginResponse {
    private String token;
    private String role;
    private Long userId;
    private String realName;

    public LoginResponse() {
    }

    public LoginResponse(String token, String role, Long userId, String realName) {
        this.token = token;
        this.role = role;
        this.userId = userId;
        this.realName = realName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}
