package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.CounselorClassScopeMapper;
import com.cqnu.eval.mapper.NoticeMapper;
import com.cqnu.eval.mapper.NoticeTargetClassMapper;
import com.cqnu.eval.mapper.UserMapper;
import com.cqnu.eval.model.dto.NoticeUpsertRequest;
import com.cqnu.eval.model.entity.NoticeEntity;
import com.cqnu.eval.model.entity.UserEntity;
import com.cqnu.eval.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class NoticeService {

    private static final String AUDIENCE_ALL_COLLEGE = "ALL_COLLEGE";
    private static final String AUDIENCE_COUNSELOR_SCOPE = "COUNSELOR_SCOPE";

    private final NoticeMapper noticeMapper;
    private final NoticeTargetClassMapper noticeTargetClassMapper;
    private final CounselorClassScopeMapper counselorClassScopeMapper;
    private final UserMapper userMapper;

    public NoticeService(NoticeMapper noticeMapper,
                         NoticeTargetClassMapper noticeTargetClassMapper,
                         CounselorClassScopeMapper counselorClassScopeMapper,
                         UserMapper userMapper) {
        this.noticeMapper = noticeMapper;
        this.noticeTargetClassMapper = noticeTargetClassMapper;
        this.counselorClassScopeMapper = counselorClassScopeMapper;
        this.userMapper = userMapper;
    }

    public List<Map<String, Object>> list(CurrentUser user, String status, String keyword) {
        String role = roleOf(user);
        String normalizedStatus = normalizeNoticeStatus(status);
        String kw = keyword == null ? null : keyword.trim();

        if ("STUDENT".equals(role)) {
            UserEntity student = requireUser(user.getId());
            String className = normalizeClassName(student.getClassName());
            return noticeMapper.listPublishedForStudent(className.isEmpty() ? null : className);
        }
        if ("COUNSELOR".equals(role)) {
            return noticeMapper.listForCounselor(user.getId(), normalizedStatus, kw);
        }
        if ("ADMIN".equals(role)) {
            return noticeMapper.list(normalizedStatus, kw);
        }
        throw new BizException(40301, "No permission to access notices");
    }

    public Map<String, Object> detail(Long id, CurrentUser user) {
        Map<String, Object> map = noticeMapper.findDetailById(id);
        if (map == null) {
            throw new BizException(40401, "Notice not found");
        }

        String role = roleOf(user);
        if ("STUDENT".equals(role)) {
            String status = String.valueOf(map.get("status"));
            if (!"PUBLISHED".equalsIgnoreCase(status)) {
                throw new BizException(40301, "No permission to view this notice");
            }
            if (!canStudentViewNotice(id, user.getId(), map)) {
                throw new BizException(40301, "No permission to view this notice");
            }
            return map;
        }

        if ("COUNSELOR".equals(role)) {
            if (!canCounselorViewNotice(user.getId(), map)) {
                throw new BizException(40301, "No permission to view this notice");
            }
            return map;
        }

        if (!"ADMIN".equals(role)) {
            throw new BizException(40301, "No permission to view this notice");
        }
        return map;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(NoticeUpsertRequest request, CurrentUser user) {
        String role = roleOf(user);
        if (!"COUNSELOR".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(40301, "No permission to access");
        }

        NoticeEntity entity = new NoticeEntity();
        entity.setTitle(request.getTitle().trim());
        entity.setContent(request.getContent().trim());
        entity.setStatus("DRAFT");
        entity.setAudienceScope(AUDIENCE_ALL_COLLEGE);
        entity.setPublisherId(user.getId());
        noticeMapper.insert(entity);
        return noticeMapper.findDetailById(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, NoticeUpsertRequest request, CurrentUser user) {
        String role = roleOf(user);
        if (!"COUNSELOR".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(40301, "No permission to access");
        }

        NoticeEntity entity = requireNotice(id);
        requireOwnerOrAdmin(entity, user);
        String status = normalizeNoticeStatus(entity.getStatus());
        if ("PUBLISHED".equals(status)) {
            throw new BizException(40003, "Published notices must be offline before update");
        }

        entity.setTitle(request.getTitle().trim());
        entity.setContent(request.getContent().trim());
        noticeMapper.updateContent(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, CurrentUser user) {
        String role = roleOf(user);
        if (!"COUNSELOR".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(40301, "No permission to access");
        }

        NoticeEntity entity = requireNotice(id);
        requireOwnerOrAdmin(entity, user);
        String status = normalizeNoticeStatus(entity.getStatus());
        if (!"DRAFT".equals(status) && !"OFFLINE".equals(status)) {
            throw new BizException(40003, "Current status cannot publish");
        }

        if ("COUNSELOR".equals(role)) {
            List<String> rawClasses = counselorClassScopeMapper.listClassNamesByCounselorId(user.getId());
            Set<String> classes = new LinkedHashSet<>();
            for (String rawClass : rawClasses) {
                String className = normalizeClassName(rawClass);
                if (!className.isEmpty()) {
                    classes.add(className);
                }
            }
            if (classes.isEmpty()) {
                throw new BizException(40001, "No assigned classes, please contact admin first");
            }

            noticeMapper.publish(id, AUDIENCE_COUNSELOR_SCOPE);
            noticeTargetClassMapper.deleteByNoticeId(id);
            for (String className : classes) {
                noticeTargetClassMapper.insert(id, className);
            }
            return;
        }

        noticeMapper.publish(id, AUDIENCE_ALL_COLLEGE);
        noticeTargetClassMapper.deleteByNoticeId(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id, CurrentUser user) {
        String role = roleOf(user);
        if (!"COUNSELOR".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(40301, "No permission to access");
        }

        NoticeEntity entity = requireNotice(id);
        requireOwnerOrAdmin(entity, user);
        String status = normalizeNoticeStatus(entity.getStatus());
        if (!"PUBLISHED".equals(status)) {
            throw new BizException(40003, "Current status cannot offline");
        }
        noticeMapper.offline(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, CurrentUser user) {
        String role = roleOf(user);
        if (!"COUNSELOR".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(40301, "No permission to access");
        }

        NoticeEntity entity = requireNotice(id);
        requireOwnerOrAdmin(entity, user);
        String status = normalizeNoticeStatus(entity.getStatus());
        if ("PUBLISHED".equals(status)) {
            throw new BizException(40003, "Published notice must be offline before delete");
        }
        noticeTargetClassMapper.deleteByNoticeId(id);
        noticeMapper.deleteById(id);
    }

    private NoticeEntity requireNotice(Long id) {
        NoticeEntity entity = noticeMapper.findById(id);
        if (entity == null) {
            throw new BizException(40401, "Notice not found");
        }
        return entity;
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new BizException(40401, "User not found");
        }
        return user;
    }

    private boolean canStudentViewNotice(Long noticeId, Long studentId, Map<String, Object> noticeMap) {
        String audienceScope = normalizeAudienceScope(String.valueOf(noticeMap.get("audience_scope")));
        if (AUDIENCE_ALL_COLLEGE.equals(audienceScope)) {
            return true;
        }

        UserEntity student = requireUser(studentId);
        String className = normalizeClassName(student.getClassName());
        if (className.isEmpty()) {
            return false;
        }
        return noticeTargetClassMapper.countByNoticeIdAndClassName(noticeId, className) > 0;
    }

    private boolean canCounselorViewNotice(Long counselorId, Map<String, Object> noticeMap) {
        Object publisherId = noticeMap.get("publisher_id");
        if (publisherId instanceof Number && ((Number) publisherId).longValue() == counselorId) {
            return true;
        }
        String publisherRole = String.valueOf(noticeMap.get("publisher_role"));
        return "ADMIN".equalsIgnoreCase(publisherRole);
    }

    private void requireOwnerOrAdmin(NoticeEntity notice, CurrentUser user) {
        String role = roleOf(user);
        if ("ADMIN".equals(role)) {
            return;
        }
        if (notice.getPublisherId() != null && notice.getPublisherId().equals(user.getId())) {
            return;
        }
        throw new BizException(40301, "No permission to operate this notice");
    }

    private String normalizeAudienceScope(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return value.isEmpty() ? AUDIENCE_ALL_COLLEGE : value;
    }

    private String normalizeClassName(String raw) {
        return raw == null ? "" : raw.trim();
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
            throw new BizException(40101, "Not logged in or missing token");
        }
        return user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
    }
}
