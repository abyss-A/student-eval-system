package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.common.RoleGuard;
import com.cqnu.eval.model.dto.ReportExportRequest;
import com.cqnu.eval.security.CurrentUser;
import com.cqnu.eval.security.UserContext;
import com.cqnu.eval.service.ExportFile;
import com.cqnu.eval.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/submissions")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{id}/report/availability")
    public ApiResponse<Map<String, Object>> availability(@PathVariable Long id) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "STUDENT");
        return ApiResponse.ok(reportService.availability(id, user));
    }

    @PostMapping("/{id}/report/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id, @RequestBody @Validated ReportExportRequest request) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireRole(user, "STUDENT");
        ExportFile file = reportService.export(id, request.getFormat(), user);
        String name = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(file.getContent());
    }
}
