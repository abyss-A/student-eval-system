package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.UserMapper;
import com.cqnu.eval.model.entity.UserEntity;
import com.cqnu.eval.security.CurrentUser;
import com.cqnu.eval.security.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserMapper userMapper;

    public MeController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        CurrentUser current = UserContext.get();
        UserEntity user = userMapper.findById(current.getId());
        if (user == null) {
            throw new BizException(40401, "用户不存在");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("role", user.getRole());
        map.put("realName", user.getRealName());
        map.put("studentNo", user.getStudentNo());
        map.put("className", user.getClassName());
        map.put("majorName", user.getMajorName());
        return ApiResponse.ok(map);
    }
}
