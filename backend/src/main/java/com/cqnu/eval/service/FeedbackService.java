package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.AttachmentMapper;
import com.cqnu.eval.mapper.FeedbackMapper;
import com.cqnu.eval.model.dto.FeedbackCreateRequest;
import com.cqnu.eval.model.dto.FeedbackHandleRequest;
import com.cqnu.eval.model.entity.AttachmentEntity;
import com.cqnu.eval.model.entity.FeedbackEntity;
import com.cqnu.eval.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FeedbackService {

    private final FeedbackMapper feedbackMapper;
    private final AttachmentMapper attachmentMapper;

    public FeedbackService(FeedbackMapper feedbackMapper, AttachmentMapper attachmentMapper) {
        this.feedbackMapper = feedbackMapper;
        this.attachmentMapper = attachmentMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public FeedbackEntity create(FeedbackCreateRequest request, CurrentUser user) {
        if (user == null) {
            throw new BizException(40101, "未登录或缺少 Token");
        }

        FeedbackEntity entity = new FeedbackEntity();
        entity.setCreatorId(user.getId());
        entity.setTitle(request.getTitle().trim());
        entity.setContent(request.getContent().trim());
        entity.setScreenshotFileIds(sanitizeScreenshotFileIds(request.getScreenshotFileIds(), user.getId()));
        entity.setStatus("NEW");
        feedbackMapper.insert(entity);
        return entity;
    }

    public List<Map<String, Object>> listMy(Long creatorId, String status) {
        return feedbackMapper.listMy(creatorId, normalizeFeedbackStatus(status));
    }

    // Global feedback list is admin-only.
    public List<Map<String, Object>> listAll(CurrentUser user, String status, String keyword) {
        requireAdmin(user);
        String kw = keyword == null ? null : keyword.trim();
        return feedbackMapper.listAll(normalizeFeedbackStatus(status), kw);
    }

    public Map<String, Object> detail(Long id, CurrentUser user) {
        if (user == null) {
            throw new BizException(40101, "未登录或缺少 Token");
        }

        Map<String, Object> map = feedbackMapper.findDetailById(id);
        if (map == null) {
            throw new BizException(40401, "反馈不存在");
        }

        // Admin can view all; others can only view their own feedback.
        String role = roleOf(user);
        if ("ADMIN".equals(role)) {
            return map;
        }

        Long creatorId = toLong(map.get("creator_id"));
        if (creatorId != null && creatorId.equals(user.getId())) {
            return map;
        }
        throw new BizException(40301, "无权限查看该反馈");
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, FeedbackHandleRequest request, CurrentUser user) {
        requireAdmin(user);

        FeedbackEntity entity = feedbackMapper.findById(id);
        if (entity == null) {
            throw new BizException(40401, "反馈不存在");
        }

        String status = normalizeFeedbackStatus(entity.getStatus());
        if ("CLOSED".equals(status)) {
            throw new BizException(40003, "该反馈已关闭，不能再处理");
        }

        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase(Locale.ROOT);
        String replyContent = request.getReplyContent() == null ? "" : request.getReplyContent().trim();

        if ("REPLY".equals(action)) {
            if (replyContent.isBlank()) {
                throw new BizException(40001, "回复内容不能为空");
            }
            feedbackMapper.handleReply(id, user.getId(), replyContent);
            return;
        }

        if ("CLOSE".equals(action)) {
            feedbackMapper.handleClose(id, user.getId(), replyContent);
            return;
        }

        throw new BizException(40001, "不支持的处理动作");
    }

    private String sanitizeScreenshotFileIds(String raw, Long creatorId) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String[] parts = raw.split(",");
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String trimmed = p.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            long id;
            try {
                id = Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                throw new BizException(40001, "截图附件ID格式不正确: " + trimmed);
            }
            if (id <= 0) {
                throw new BizException(40001, "截图附件ID必须为正数");
            }
            set.add(id);
        }

        if (set.size() > 6) {
            throw new BizException(40001, "每条反馈最多上传6张截图");
        }

        // Only allow referencing images uploaded by the creator.
        for (Long id : set) {
            AttachmentEntity meta = attachmentMapper.findById(id);
            if (meta == null) {
                throw new BizException(40401, "截图附件不存在: " + id);
            }
            if (meta.getUploaderId() == null || !meta.getUploaderId().equals(creatorId)) {
                throw new BizException(40301, "只能引用自己上传的截图");
            }

            String mime = meta.getMimeType();
            if (mime != null) {
                mime = mime.trim().toLowerCase(Locale.ROOT);
                int semi = mime.indexOf(';');
                if (semi > 0) {
                    mime = mime.substring(0, semi).trim();
                }
            }
            boolean allowedByMime = "image/jpeg".equals(mime) || "image/png".equals(mime);

            String fileName = meta.getFileName();
            String nameLower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
            boolean allowedByExt = nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg") || nameLower.endsWith(".png");

            if (!allowedByMime && !allowedByExt) {
                throw new BizException(40001, "反馈截图仅支持JPG/PNG图片");
            }
        }

        List<Long> ids = new ArrayList<>(set);
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(id);
        }
        return sb.toString();
    }

    private String normalizeFeedbackStatus(String status) {
        if (status == null) {
            return null;
        }
        String s = status.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) {
            return null;
        }
        return s;
    }

    private void requireAdmin(CurrentUser user) {
        String role = roleOf(user);
        if (!"ADMIN".equals(role)) {
            throw new BizException(40301, "无权限访问");
        }
    }

    private String roleOf(CurrentUser user) {
        if (user == null) {
            throw new BizException(40101, "未登录或缺少 Token");
        }
        return user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }
}

