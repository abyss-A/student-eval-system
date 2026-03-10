package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.common.RoleGuard;
import com.cqnu.eval.model.dto.AdminAccountCreateRequest;
import com.cqnu.eval.model.dto.AdminAccountEnabledRequest;
import com.cqnu.eval.model.dto.AdminAccountImportCommitRequest;
import com.cqnu.eval.model.dto.AdminAccountUpdateRequest;
import com.cqnu.eval.security.UserContext;
import com.cqnu.eval.service.AdminAccountService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listAccounts(@RequestParam(required = false) String role,
                                                               @RequestParam(required = false) Integer enabled,
                                                               @RequestParam(required = false) String keyword) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(adminAccountService.listAccounts(role, enabled, keyword));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createAccount(@RequestBody @Validated AdminAccountCreateRequest request) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(adminAccountService.createAccount(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> updateAccount(@PathVariable Long id,
                                                          @RequestBody @Validated AdminAccountUpdateRequest request) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(adminAccountService.updateAccount(id, request));
    }

    @PutMapping("/{id}/enabled")
    public ApiResponse<Map<String, Object>> setEnabled(@PathVariable Long id,
                                                       @RequestBody @Validated AdminAccountEnabledRequest request) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(adminAccountService.setEnabled(id, Boolean.TRUE.equals(request.getEnabled())));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Map<String, Object>> resetPassword(@PathVariable Long id) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(adminAccountService.resetPassword(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteAccount(@PathVariable Long id) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        adminAccountService.deleteAccount(id);
        return ApiResponse.ok("删除成功");
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate(@RequestParam String role) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        String normalizedRole = String.valueOf(role).trim().toUpperCase();
        String fileName = "STUDENT".equals(normalizedRole)
                ? "student-account-import-template.xlsx"
                : "counselor-account-import-template.xlsx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        byte[] content = adminAccountService.buildImportTemplate(role);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> previewImport(@RequestParam String role,
                                                          @RequestPart("file") MultipartFile file) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(adminAccountService.previewImport(role, file));
    }

    @PostMapping("/import/commit")
    public ApiResponse<Map<String, Object>> commitImport(@RequestBody @Validated AdminAccountImportCommitRequest request) {
        RoleGuard.requireRole(UserContext.get(), "ADMIN");
        return ApiResponse.ok(adminAccountService.commitImport(request.getPreviewToken()));
    }
}
