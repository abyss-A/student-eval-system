package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.common.RoleGuard;
import com.cqnu.eval.model.dto.CounselorScopeUpdateRequest;
import com.cqnu.eval.security.CurrentUser;
import com.cqnu.eval.security.UserContext;
import com.cqnu.eval.service.CounselorScopeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCounselorScopeController {

    private final CounselorScopeService counselorScopeService;

    public AdminCounselorScopeController(CounselorScopeService counselorScopeService) {
        this.counselorScopeService = counselorScopeService;
    }

    @GetMapping("/counselors")
    public ApiResponse<List<Map<String, Object>>> listCounselors(@RequestParam(required = false) String keyword) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "ADMIN");
        return ApiResponse.ok(counselorScopeService.listCounselors(keyword));
    }

    @GetMapping("/classes")
    public ApiResponse<List<Map<String, Object>>> listClasses() {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "ADMIN");
        return ApiResponse.ok(counselorScopeService.listAvailableClasses());
    }

    @GetMapping("/counselors/{counselorId}/scopes")
    public ApiResponse<Map<String, Object>> getScopes(@PathVariable Long counselorId) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "ADMIN");
        return ApiResponse.ok(counselorScopeService.getCounselorScopes(counselorId));
    }

    @PutMapping("/counselors/{counselorId}/scopes")
    public ApiResponse<Map<String, Object>> replaceScopes(@PathVariable Long counselorId,
                                                           @RequestBody @Validated CounselorScopeUpdateRequest request) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "ADMIN");
        return ApiResponse.ok(counselorScopeService.replaceCounselorScopes(counselorId, request.getClassNames(), user.getId()));
    }
}
