package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.AttachmentMapper;
import com.cqnu.eval.model.entity.AttachmentEntity;
import com.cqnu.eval.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    private final AttachmentMapper attachmentMapper;

    public FileService(AttachmentMapper attachmentMapper) {
        this.attachmentMapper = attachmentMapper;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
    }

    @Transactional(rollbackFor = Exception.class)
    public AttachmentEntity upload(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(40001, "文件不能为空");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String extLower = ext.toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        String normalizedType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        int semi = normalizedType.indexOf(';');
        if (semi > 0) {
            normalizedType = normalizedType.substring(0, semi).trim();
        }
        boolean allowedByMime = !normalizedType.isBlank()
                && ("image/jpeg".equals(normalizedType) || "image/png".equals(normalizedType));
        boolean allowedByExt = ".jpg".equals(extLower) || ".jpeg".equals(extLower) || ".png".equals(extLower);
        if (!allowedByMime && !allowedByExt) {
            throw new BizException(40001, "仅支持上传 JPG/PNG 图片");
        }
        if (original == null || original.isBlank()) {
            original = "image" + extLower;
        }
        String realName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = Paths.get(uploadDir, realName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException(50001, "文件保存失败: " + e.getMessage());
        }

        AttachmentEntity entity = new AttachmentEntity();
        entity.setBizType("UNBOUND");
        entity.setBizId(0L);
        entity.setUploaderId(userId);
        entity.setFileName(original);
        entity.setFilePath(target.toString());
        entity.setFileSize(file.getSize());
        String storedMime = normalizedType;
        if (storedMime.isBlank()) {
            if (".png".equals(extLower)) {
                storedMime = "image/png";
            } else if (".jpg".equals(extLower) || ".jpeg".equals(extLower)) {
                storedMime = "image/jpeg";
            } else {
                storedMime = null;
            }
        }
        entity.setMimeType(storedMime);
        attachmentMapper.insert(entity);
        return entity;
    }

    public Resource download(Long id, CurrentUser user) {
        AttachmentEntity meta = requireMetaAllowed(id, user);
        return new FileSystemResource(meta.getFilePath());
    }

    public AttachmentEntity findMeta(Long id, CurrentUser user) {
        return requireMetaAllowed(id, user);
    }

    private AttachmentEntity requireMetaAllowed(Long id, CurrentUser user) {
        AttachmentEntity entity = attachmentMapper.findById(id);
        if (entity == null) {
            throw new BizException(40401, "文件不存在");
        }
        if (user == null) {
            throw new BizException(40101, "未登录");
        }

        Long userId = user.getId();
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);

        if (entity.getUploaderId() != null && entity.getUploaderId().equals(userId)) {
            return entity;
        }

        if ("COUNSELOR".equals(role) || "ADMIN".equals(role)) {
            boolean referenced = attachmentMapper.countReferencedByCourse(id) > 0
                    || attachmentMapper.countReferencedByActivity(id) > 0;
            if (referenced) {
                return entity;
            }
        }

        throw new BizException(40301, "无权限访问该文件");
    }
}
