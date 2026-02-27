package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.common.RoleGuard;
import com.cqnu.eval.model.entity.AttachmentEntity;
import com.cqnu.eval.security.CurrentUser;
import com.cqnu.eval.security.UserContext;
import com.cqnu.eval.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> upload(@RequestPart("file") MultipartFile file) {
        CurrentUser user = UserContext.get();
        RoleGuard.requireAnyRole(user, "STUDENT", "COUNSELOR", "ADMIN");
        AttachmentEntity entity = fileService.upload(file, user.getId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("fileName", entity.getFileName());
        map.put("size", entity.getFileSize());
        return ApiResponse.ok(map);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        CurrentUser user = UserContext.get();
        Resource resource = fileService.download(id, user);
        AttachmentEntity meta = fileService.findMeta(id, user);
        String fileName = (meta.getFileName() == null || meta.getFileName().isBlank())
                ? ("file_" + meta.getId())
                : meta.getFileName();
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        String type = (meta.getMimeType() == null || meta.getMimeType().isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : meta.getMimeType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(type))
                .body(resource);
    }

    @GetMapping("/{id}/meta")
    public ApiResponse<Map<String, Object>> meta(@PathVariable Long id) {
        CurrentUser user = UserContext.get();
        AttachmentEntity meta = fileService.findMeta(id, user);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", meta.getId());
        map.put("fileName", meta.getFileName());
        map.put("size", meta.getFileSize());
        map.put("mimeType", meta.getMimeType());
        return ApiResponse.ok(map);
    }

    public static class BatchMetaRequest {
        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }

    @PostMapping("/metas")
    public ApiResponse<List<Map<String, Object>>> metas(@RequestBody @Validated BatchMetaRequest request) {
        CurrentUser user = UserContext.get();
        List<Long> ids = request.getIds();
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.ok(List.of());
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            AttachmentEntity meta = fileService.findMeta(id, user);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", meta.getId());
            map.put("fileName", meta.getFileName());
            map.put("size", meta.getFileSize());
            map.put("mimeType", meta.getMimeType());
            list.add(map);
        }
        return ApiResponse.ok(list);
    }
}
