package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.AttachmentMapper;
import com.cqnu.eval.model.entity.AttachmentEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
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
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
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
        entity.setMimeType(file.getContentType());
        attachmentMapper.insert(entity);
        return entity;
    }

    public Resource download(Long id, Long userId) {
        AttachmentEntity entity = attachmentMapper.findById(id);
        if (entity == null) {
            throw new BizException(40401, "文件不存在");
        }
        if (!entity.getUploaderId().equals(userId)) {
            throw new BizException(40301, "无权限下载该文件");
        }
        return new FileSystemResource(entity.getFilePath());
    }

    public AttachmentEntity findById(Long id) {
        AttachmentEntity entity = attachmentMapper.findById(id);
        if (entity == null) {
            throw new BizException(40401, "文件不存在");
        }
        return entity;
    }
}
