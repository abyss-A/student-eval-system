package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.CounselorClassScopeMapper;
import com.cqnu.eval.mapper.UserMapper;
import com.cqnu.eval.model.entity.CounselorClassScopeEntity;
import com.cqnu.eval.model.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CounselorScopeService {

    private final UserMapper userMapper;
    private final CounselorClassScopeMapper counselorClassScopeMapper;

    public CounselorScopeService(UserMapper userMapper,
                                 CounselorClassScopeMapper counselorClassScopeMapper) {
        this.userMapper = userMapper;
        this.counselorClassScopeMapper = counselorClassScopeMapper;
    }

    public List<Map<String, Object>> listCounselors(String keyword) {
        String kw = keyword == null ? null : keyword.trim();
        return userMapper.listCounselors((kw == null || kw.isEmpty()) ? null : kw);
    }

    public List<Map<String, Object>> listAvailableClasses() {
        List<String> classes = userMapper.listDistinctStudentClasses();
        List<Map<String, Object>> out = new ArrayList<>();
        for (String className : classes) {
            String normalized = normalizeClassName(className);
            if (normalized.isEmpty()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("className", normalized);
            out.add(row);
        }
        return out;
    }

    public Map<String, Object> getCounselorScopes(Long counselorId) {
        UserEntity counselor = requireCounselor(counselorId);
        List<String> assigned = counselorClassScopeMapper.listClassNamesByCounselorId(counselorId);

        List<String> classNames = new ArrayList<>();
        for (String raw : assigned) {
            String normalized = normalizeClassName(raw);
            if (!normalized.isEmpty()) {
                classNames.add(normalized);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("counselorId", counselor.getId());
        out.put("accountNo", counselor.getAccountNo());
        out.put("realName", counselor.getRealName());
        out.put("classNames", classNames);
        out.put("scopeCount", classNames.size());
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> replaceCounselorScopes(Long counselorId, List<String> classNames, Long assignedBy) {
        requireCounselor(counselorId);

        UserEntity admin = userMapper.findById(assignedBy);
        if (admin == null || !"ADMIN".equalsIgnoreCase(String.valueOf(admin.getRole()))) {
            throw new BizException(40301, "Only admin can assign counselor class scopes");
        }

        Set<String> normalizedClassSet = new LinkedHashSet<>();
        if (classNames != null) {
            for (String raw : classNames) {
                String normalized = normalizeClassName(raw);
                if (!normalized.isEmpty()) {
                    normalizedClassSet.add(normalized);
                }
            }
        }

        Set<String> validClasses = new LinkedHashSet<>();
        for (String className : userMapper.listDistinctStudentClasses()) {
            String normalized = normalizeClassName(className);
            if (!normalized.isEmpty()) {
                validClasses.add(normalized);
            }
        }

        for (String className : normalizedClassSet) {
            if (!validClasses.contains(className)) {
                throw new BizException(40001, "Unknown class: " + className);
            }
        }

        if (!normalizedClassSet.isEmpty()) {
            List<String> targetClasses = new ArrayList<>(normalizedClassSet);
            List<Map<String, Object>> conflicts = counselorClassScopeMapper.listConflicts(targetClasses, counselorId);
            if (conflicts != null && !conflicts.isEmpty()) {
                StringBuilder sb = new StringBuilder("班级归属冲突，请刷新后重试：");
                for (int i = 0; i < conflicts.size(); i++) {
                    Map<String, Object> row = conflicts.get(i);
                    String className = normalizeClassName(String.valueOf(row.get("className")));
                    String ownerName = String.valueOf(row.get("counselorName"));
                    String ownerAccountNo = String.valueOf(row.get("counselorAccountNo"));
                    if (i > 0) {
                        sb.append("；");
                    }
                    sb.append(className).append(" -> ").append(ownerName).append("(").append(ownerAccountNo).append(")");
                }
                throw new BizException(40901, sb.toString());
            }
        }

        counselorClassScopeMapper.deleteByCounselorId(counselorId);
        for (String className : normalizedClassSet) {
            CounselorClassScopeEntity entity = new CounselorClassScopeEntity();
            entity.setCounselorId(counselorId);
            entity.setClassName(className);
            entity.setAssignedBy(assignedBy);
            counselorClassScopeMapper.insert(entity);
        }

        return getCounselorScopes(counselorId);
    }

    private UserEntity requireCounselor(Long counselorId) {
        UserEntity user = userMapper.findById(counselorId);
        if (user == null) {
            throw new BizException(40401, "Counselor not found");
        }
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        if (!"COUNSELOR".equals(role)) {
            throw new BizException(40001, "Target user is not counselor");
        }
        if (user.getEnabled() != null && user.getEnabled() == 0) {
            throw new BizException(40001, "Target counselor is disabled");
        }
        return user;
    }

    private String normalizeClassName(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
