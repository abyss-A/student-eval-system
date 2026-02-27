package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.model.dto.FeedbackCreateRequest;
import com.cqnu.eval.model.dto.FeedbackHandleRequest;
import com.cqnu.eval.model.entity.FeedbackEntity;
import com.cqnu.eval.security.CurrentUser;
import com.cqnu.eval.security.UserContext;
import com.cqnu.eval.service.FeedbackService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<FeedbackEntity> create(@RequestBody @Validated FeedbackCreateRequest request) {
        CurrentUser user = UserContext.get();
        return ApiResponse.ok(feedbackService.create(request, user));
    }

    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> my(@RequestParam(required = false) String status) {
        CurrentUser user = UserContext.get();
        return ApiResponse.ok(feedbackService.listMy(user.getId(), status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        CurrentUser user = UserContext.get();
        return ApiResponse.ok(feedbackService.detail(id, user));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String keyword) {
        CurrentUser user = UserContext.get();
        return ApiResponse.ok(feedbackService.listAll(user, status, keyword));
    }

    @PostMapping("/{id}/handle")
    public ApiResponse<Void> handle(@PathVariable Long id, @RequestBody @Validated FeedbackHandleRequest request) {
        CurrentUser user = UserContext.get();
        feedbackService.handle(id, request, user);
        return ApiResponse.ok(null);
    }
}

