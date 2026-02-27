package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.model.dto.NoticeUpsertRequest;
import com.cqnu.eval.security.CurrentUser;
import com.cqnu.eval.security.UserContext;
import com.cqnu.eval.service.NoticeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String keyword) {
        CurrentUser user = UserContext.get();
        return ApiResponse.ok(noticeService.list(user, status, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        CurrentUser user = UserContext.get();
        return ApiResponse.ok(noticeService.detail(id, user));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody @Validated NoticeUpsertRequest request) {
        CurrentUser user = UserContext.get();
        return ApiResponse.ok(noticeService.create(request, user));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Validated NoticeUpsertRequest request) {
        CurrentUser user = UserContext.get();
        noticeService.update(id, request, user);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        CurrentUser user = UserContext.get();
        noticeService.publish(id, user);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/offline")
    public ApiResponse<Void> offline(@PathVariable Long id) {
        CurrentUser user = UserContext.get();
        noticeService.offline(id, user);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CurrentUser user = UserContext.get();
        noticeService.delete(id, user);
        return ApiResponse.ok(null);
    }
}

