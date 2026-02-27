package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.NoticeMapper;
import com.cqnu.eval.model.dto.NoticeUpsertRequest;
import com.cqnu.eval.model.entity.NoticeEntity;
import com.cqnu.eval.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    public List<Map<String, Object>> list(CurrentUser user, String status, String keyword) {
        String role = roleOf(user);
        if ("STUDENT".equals(role)) {
            return noticeMapper.listPublished();
        }

        String normalizedStatus = normalizeNoticeStatus(status);
        String kw = keyword == null ? null : keyword.trim();
        return noticeMapper.list(normalizedStatus, kw);
    }

    public Map<String, Object> detail(Long id, CurrentUser user) {
        Map<String, Object> map = noticeMapper.findDetailById(id);
        if (map == null) {
            throw new BizException(40401, "公告不存在");
        }

        String role = roleOf(user);
        if ("STUDENT".equals(role)) {
            String status = String.valueOf(map.get("status"));
            if (!"PUBLISHED".equalsIgnoreCase(status)) {
                throw new BizException(40301, "无权限查看该公告");
            }
        }
        return map;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(NoticeUpsertRequest request, CurrentUser user) {
        String role = roleOf(user);
        if (!"COUNSELOR".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(40301, "无权限访问");
        }

        NoticeEntity entity = new NoticeEntity();
        entity.setTitle(request.getTitle().trim());
        entity.setContent(request.getContent().trim());
        entity.setStatus("DRAFT");
        entity.setPublisherId(user.getId());
        noticeMapper.insert(entity);
        return noticeMapper.findDetailById(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, NoticeUpsertRequest request, CurrentUser user) {
        String role = roleOf(user);
        if (!"COUNSELOR".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(40301, "无权限访问");
        }

        NoticeEntity entity = requireNotice(id);
        requireOwnerOrAdmin(entity, user);
        String status = normalizeNoticeStatus(entity.getStatus());
        if ("PUBLISHED".equals(status)) {
            throw new BizException(40003, "已发布公告请先下线再修改");
        }

        entity.setTitle(request.getTitle().trim());
        entity.setContent(request.getContent().trim());
        noticeMapper.updateContent(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, CurrentUser user) {
        String role = roleOf(user);
        if (!"COUNSELOR".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(40301, "无权限访问");
        }

        NoticeEntity entity = requireNotice(id);
        requireOwnerOrAdmin(entity, user);
        String status = normalizeNoticeStatus(entity.getStatus());
        if (!"DRAFT".equals(status) && !"OFFLINE".equals(status)) {
            throw new BizException(40003, "当前状态不允许发布");
        }
        noticeMapper.publish(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id, CurrentUser user) {
        String role = roleOf(user);
        if (!"COUNSELOR".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(40301, "无权限访问");
        }

        NoticeEntity entity = requireNotice(id);
        requireOwnerOrAdmin(entity, user);
        String status = normalizeNoticeStatus(entity.getStatus());
        if (!"PUBLISHED".equals(status)) {
            throw new BizException(40003, "当前状态不允许下线");
        }
        noticeMapper.offline(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, CurrentUser user) {
        String role = roleOf(user);
        if (!"COUNSELOR".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(40301, "无权限访问");
        }

        NoticeEntity entity = requireNotice(id);
        requireOwnerOrAdmin(entity, user);
        String status = normalizeNoticeStatus(entity.getStatus());
        if ("PUBLISHED".equals(status)) {
            throw new BizException(40003, "已发布公告不允许直接删除，请先下线");
        }
        noticeMapper.deleteById(id);
    }

    private NoticeEntity requireNotice(Long id) {
        NoticeEntity entity = noticeMapper.findById(id);
        if (entity == null) {
            throw new BizException(40401, "公告不存在");
        }
        return entity;
    }

    private void requireOwnerOrAdmin(NoticeEntity notice, CurrentUser user) {
        String role = roleOf(user);
        if ("ADMIN".equals(role)) {
            return;
        }
        if (notice.getPublisherId() != null && notice.getPublisherId().equals(user.getId())) {
            return;
        }
        throw new BizException(40301, "无权限操作该公告");
    }

    private String normalizeNoticeStatus(String status) {
        if (status == null) {
            return null;
        }
        String s = status.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) {
            return null;
        }
        return s;
    }

    private String roleOf(CurrentUser user) {
        if (user == null) {
            throw new BizException(40101, "未登录或缺少 Token");
        }
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        return role;
    }
}

