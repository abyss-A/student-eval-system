package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.common.RoleGuard;
import com.cqnu.eval.model.entity.SubmissionEntity;
import com.cqnu.eval.security.UserContext;
import com.cqnu.eval.service.SubmissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final SubmissionService submissionService;

    public AdminController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> tasks() {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(submissionService.listCounselorReviewedTasks());
    }

    @PostMapping("/submissions/{id}/finalize")
    public ApiResponse<SubmissionEntity> finalizeSubmission(@PathVariable Long id) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(submissionService.finalizeSubmission(id));
    }
}
