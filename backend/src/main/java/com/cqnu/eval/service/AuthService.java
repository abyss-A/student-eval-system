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

import java.util.Locale;

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
        String password = request.getPassword().trim();
        String realName = request.getRealName().trim();
        String gender = request.getGender().trim();
        String accountNo = request.getAccountNo().trim();
        String gradeClass = request.getGradeClass().trim();
        String phone = request.getPhone().trim();

        String normalizedGender = normalizeGender(gender);
        if (normalizedGender.isEmpty()) {
            throw new BizException(40001, "性别仅支持：男、女");
        }

        if (userMapper.findByAccountNo(accountNo) != null) {
            throw new BizException(40001, "学号/工号已被注册");
        }

        UserEntity user = new UserEntity();
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("STUDENT");
        user.setAccountNo(accountNo);
        user.setRealName(realName);
        user.setGender(normalizedGender);
        user.setPhone(phone);
        user.setClassName(gradeClass);
        user.setEnabled(1);
        userMapper.insert(user);

        return new RegisterStudentResponse(user.getId(), user.getAccountNo(), user.getRealName());
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

    private String normalizeGender(String raw) {
        String g = raw == null ? "" : raw.trim();
        if (g.isEmpty()) return "";
        String up = g.toUpperCase(Locale.ROOT);
        if ("男".equals(g) || "MALE".equals(up) || "M".equals(up)) {
            return "男";
        }
        if ("女".equals(g) || "FEMALE".equals(up) || "F".equals(up)) {
            return "女";
        }
        return "";
    }
}
