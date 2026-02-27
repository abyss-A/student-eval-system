package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.common.RoleGuard;
import com.cqnu.eval.model.dto.BatchActivityRequest;
import com.cqnu.eval.model.dto.BatchCourseRequest;
import com.cqnu.eval.model.entity.SubmissionEntity;
import com.cqnu.eval.security.CurrentUser;
import com.cqnu.eval.security.UserContext;
import com.cqnu.eval.service.SubmissionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/submissions")
    public ApiResponse<SubmissionEntity> createOrGet() {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "STUDENT");
        return ApiResponse.ok(submissionService.createOrGetMySubmission(user.getId()));
    }

    @GetMapping("/submissions/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(submissionService.getSubmissionDetail(id, UserContext.get()));
    }

    @PutMapping("/submissions/{id}/courses/batch")
    public ApiResponse<Void> saveCourses(@PathVariable Long id, @RequestBody @Validated BatchCourseRequest request) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "STUDENT");
        submissionService.saveCourses(id, user.getId(), request);
        return ApiResponse.ok(null);
    }

    @PutMapping("/submissions/{id}/activities/batch")
    public ApiResponse<Void> saveActivities(@PathVariable Long id, @RequestBody @Validated BatchActivityRequest request) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "STUDENT");
        submissionService.saveActivities(id, user.getId(), request);
        return ApiResponse.ok(null);
    }

    @PutMapping("/submissions/{id}/activities/module/{moduleType}")
    public ApiResponse<Void> saveActivitiesByModule(@PathVariable Long id,
                                                    @PathVariable String moduleType,
                                                    @RequestBody @Validated BatchActivityRequest request) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "STUDENT");
        submissionService.saveActivitiesByModule(id, user.getId(), moduleType, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/submissions/{id}/submit")
    public ApiResponse<SubmissionEntity> submit(@PathVariable Long id) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "STUDENT");
        return ApiResponse.ok(submissionService.submit(id, user.getId()));
    }

    @GetMapping("/submissions/{id}/score")
    public ApiResponse<Map<String, Object>> score(@PathVariable Long id) {
        return ApiResponse.ok(submissionService.getScore(id, UserContext.get()));
    }

    @GetMapping("/rankings")
    public ApiResponse<List<Map<String, Object>>> ranking(@RequestParam Long semesterId) {
        return ApiResponse.ok(submissionService.getRanking(semesterId));
    }
}
