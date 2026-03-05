package com.cqnu.eval.security;

public class CurrentUser {
    private Long id;
    private String accountNo;
    private String role;

    public CurrentUser(Long id, String accountNo, String role) {
        this.id = id;
        this.accountNo = accountNo;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public String getRole() {
        return role;
    }
}
