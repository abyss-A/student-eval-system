package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.common.RoleGuard;
import com.cqnu.eval.model.dto.ReviewDecisionRequest;
import com.cqnu.eval.security.CurrentUser;
import com.cqnu.eval.security.UserContext;
import com.cqnu.eval.service.ReviewService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> tasks() {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "COUNSELOR");
        return ApiResponse.ok(reviewService.listTasks(user.getId()));
    }

    @GetMapping("/class-overview")
    public ApiResponse<List<Map<String, Object>>> classOverview() {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "COUNSELOR");
        return ApiResponse.ok(reviewService.listClassOverview(user.getId()));
    }

    @PostMapping("/items/{itemType}/{itemId}/decision")
    public ApiResponse<Void> decision(@PathVariable String itemType,
                                      @PathVariable Long itemId,
                                      @RequestBody @Validated ReviewDecisionRequest request) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "COUNSELOR");
        reviewService.decision(itemType, itemId, request, user.getId());
        return ApiResponse.ok(null);
    }

    @PostMapping("/submissions/{submissionId}/submit")
    public ApiResponse<Void> submitToAdmin(@PathVariable Long submissionId) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "COUNSELOR");
        reviewService.submitToAdmin(submissionId, user.getId());
        return ApiResponse.ok(null);
    }
}
