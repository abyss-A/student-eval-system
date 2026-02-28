package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.model.dto.LoginRequest;
import com.cqnu.eval.model.dto.LoginResponse;
import com.cqnu.eval.model.dto.RegisterStudentRequest;
import com.cqnu.eval.model.dto.RegisterStudentResponse;
import com.cqnu.eval.service.AuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/register/student")
    public ApiResponse<RegisterStudentResponse> registerStudent(@RequestBody @Validated RegisterStudentRequest request) {
        return ApiResponse.ok(authService.registerStudent(request));
    }
}
