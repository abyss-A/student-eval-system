package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.common.RoleGuard;
import com.cqnu.eval.model.dto.AdminSemesterCreateRequest;
import com.cqnu.eval.model.entity.SemesterEntity;
import com.cqnu.eval.security.UserContext;
import com.cqnu.eval.service.SemesterService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/semesters")
public class AdminSemesterController {

    private final SemesterService semesterService;

    public AdminSemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> overview() {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(semesterService.getAdminOverview());
    }

    @PostMapping
    public ApiResponse<SemesterEntity> create(@RequestBody @Validated AdminSemesterCreateRequest request) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(semesterService.createSemester(request));
    }

    @PutMapping("/{id}/active")
    public ApiResponse<SemesterEntity> activate(@PathVariable Long id) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(semesterService.activateSemester(id));
    }
}

