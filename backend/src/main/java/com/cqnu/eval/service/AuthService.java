package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.UserMapper;
import com.cqnu.eval.model.dto.LoginRequest;
import com.cqnu.eval.model.dto.LoginResponse;
import com.cqnu.eval.model.dto.RegisterStudentRequest;
import com.cqnu.eval.model.dto.RegisterStudentResponse;
import com.cqnu.eval.model.entity.UserEntity;
import com.cqnu.eval.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserMapper userMapper, JwtUtils jwtUtils, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        String accountNo = request.getAccountNo() == null ? "" : request.getAccountNo().trim();
        UserEntity user = userMapper.findEnabledByAccountNo(accountNo);
        if (user == null || !verifyPasswordAndUpgradeIfNeeded(user, request.getPassword())) {
            throw new BizException(40101, "学号/工号或密码错误");
        }
        String token = jwtUtils.createToken(user.getId(), user.getAccountNo(), user.getRole());
        return new LoginResponse(token, user.getRole(), user.getId(), user.getRealName());
    }

    public RegisterStudentResponse registerStudent(RegisterStudentRequest request) {
        throw new BizException(40301, "当前系统账号由管理员统一创建，请联系管理员开通账号");
    }

    private boolean verifyPasswordAndUpgradeIfNeeded(UserEntity user, String rawPassword) {
        String stored = user.getPasswordHash();
        if (stored == null || stored.isEmpty()) {
            return false;
        }

        if (isBcryptHash(stored)) {
            return passwordEncoder.matches(rawPassword, stored);
        }

        if (!stored.equals(rawPassword)) {
            return false;
        }

        userMapper.updatePasswordById(user.getId(), passwordEncoder.encode(rawPassword));
        return true;
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
