package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Account no cannot be blank")
    @Size(min = 4, max = 32, message = "Account no length must be 4-32")
    private String accountNo;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
