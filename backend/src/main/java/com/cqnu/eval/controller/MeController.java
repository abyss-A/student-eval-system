package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.UserMapper;
import com.cqnu.eval.model.dto.ChangePasswordRequest;
import com.cqnu.eval.model.dto.MeProfileUpdateRequest;
import com.cqnu.eval.model.entity.UserEntity;
import com.cqnu.eval.security.CurrentUser;
import com.cqnu.eval.security.UserContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public MeController(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        UserEntity user = requireCurrentUser();
        return ApiResponse.ok(buildProfileMap(user));
    }

    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@RequestBody @Validated MeProfileUpdateRequest request) {
        UserEntity user = requireCurrentUser();
        String phone = request.getPhone().trim();
        userMapper.updatePhoneById(user.getId(), phone);
        user.setPhone(phone);
        return ApiResponse.ok(buildProfileMap(user));
    }

    @PostMapping("/password")
    public ApiResponse<String> changePassword(@RequestBody @Validated ChangePasswordRequest request) {
        UserEntity user = requireCurrentUser();

        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        if (!newPassword.equals(confirmPassword)) {
            throw new BizException(40001, "两次输入的新密码不一致");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BizException(40001, "新密码不能与旧密码相同");
        }
        if (!verifyPasswordAndUpgradeIfNeeded(user, oldPassword)) {
            throw new BizException(40001, "旧密码错误");
        }

        userMapper.updatePasswordById(user.getId(), passwordEncoder.encode(newPassword));
        return ApiResponse.ok("密码修改成功");
    }

    private UserEntity requireCurrentUser() {
        CurrentUser current = UserContext.get();
        UserEntity user = userMapper.findById(current.getId());
        if (user == null) {
            throw new BizException(40401, "用户不存在");
        }
        return user;
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

    private Map<String, Object> buildProfileMap(UserEntity user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("role", user.getRole());
        map.put("realName", user.getRealName());
        map.put("studentNo", user.getStudentNo());
        map.put("gender", user.getGender());
        map.put("phone", user.getPhone());
        map.put("className", user.getClassName());
        map.put("majorName", user.getMajorName());
        return map;
    }
}
