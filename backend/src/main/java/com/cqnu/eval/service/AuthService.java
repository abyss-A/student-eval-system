package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.UserMapper;
import com.cqnu.eval.model.dto.LoginRequest;
import com.cqnu.eval.model.dto.LoginResponse;
import com.cqnu.eval.model.dto.RegisterStudentRequest;
import com.cqnu.eval.model.dto.RegisterStudentResponse;
import com.cqnu.eval.model.entity.UserEntity;
import com.cqnu.eval.security.JwtUtils;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    public AuthService(UserMapper userMapper, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity user = userMapper.findByUsername(request.getUsername());
        if (user == null || !request.getPassword().equals(user.getPasswordHash())) {
            throw new BizException(40101, "用户名或密码错误");
        }
        String token = jwtUtils.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getRole(), user.getId(), user.getRealName());
    }

    public RegisterStudentResponse registerStudent(RegisterStudentRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword().trim();
        String realName = request.getRealName().trim();
        String gender = request.getGender().trim();
        String studentNo = request.getStudentNo().trim();
        String gradeClass = request.getGradeClass().trim();
        String phone = request.getPhone().trim();
        String majorName = request.getMajorName() == null ? "" : request.getMajorName().trim();

        String normalizedGender = normalizeGender(gender);
        if (normalizedGender.isEmpty()) {
            throw new BizException(40001, "性别仅支持：男、女");
        }

        if (userMapper.findByUsername(username) != null) {
            throw new BizException(40001, "用户名已存在，请更换后再试");
        }
        if (userMapper.findByStudentNo(studentNo) != null) {
            throw new BizException(40001, "学号已被注册");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(password);
        user.setRole("STUDENT");
        user.setStudentNo(studentNo);
        user.setRealName(realName);
        user.setGender(normalizedGender);
        user.setPhone(phone);
        user.setClassName(gradeClass);
        user.setMajorName(majorName.isEmpty() ? null : majorName);
        user.setEnabled(1);
        userMapper.insert(user);

        return new RegisterStudentResponse(user.getId(), user.getUsername(), user.getRealName());
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
