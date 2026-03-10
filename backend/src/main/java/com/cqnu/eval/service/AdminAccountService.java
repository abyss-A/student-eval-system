package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.UserMapper;
import com.cqnu.eval.model.dto.AdminAccountCreateRequest;
import com.cqnu.eval.model.dto.AdminAccountUpdateRequest;
import com.cqnu.eval.model.entity.UserEntity;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAccountService {

    private static final Set<String> MANAGED_ROLES = Set.of("STUDENT", "COUNSELOR");
    private static final long PREVIEW_TTL_MILLIS = 30L * 60L * 1000L;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final DataFormatter dataFormatter = new DataFormatter();
    private final Map<String, ImportPreviewSession> previewSessions = new ConcurrentHashMap<>();

    @Value("${app.account.default-password:123456}")
    private String defaultPassword;

    public AdminAccountService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Map<String, Object>> listAccounts(String role, Integer enabled, String keyword) {
        purgeExpiredPreviewSessions();
        String normalizedRole = normalizeRole(role, true);
        String normalizedKeyword = trimToNull(keyword);
        List<Map<String, Object>> rows = userMapper.listManagedAccounts(normalizedRole, enabled, normalizedKeyword);
        for (Map<String, Object> row : rows) {
            Long userId = toLong(row.get("id"));
            String reason = resolveDeleteBlockReason(userId);
            row.put("canDelete", reason == null);
            row.put("deleteBlockReason", reason);
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> createAccount(AdminAccountCreateRequest request) {
        String role = normalizeRole(request.getRole(), false);
        String accountNo = normalizeAccountNo(request.getAccountNo());
        String realName = normalizeRealName(request.getRealName());
        String gender = normalizeGender(request.getGender());
        String phone = normalizePhone(request.getPhone());
        String className = normalizeClassName(request.getClassName());

        validateClassName(role, className);
        ensureAccountNoAvailable(accountNo);

        UserEntity entity = new UserEntity();
        entity.setPasswordHash(passwordEncoder.encode(defaultPassword));
        entity.setRole(role);
        entity.setAccountNo(accountNo);
        entity.setRealName(realName);
        entity.setGender(gender);
        entity.setPhone(phone);
        entity.setClassName("STUDENT".equals(role) ? className : null);
        entity.setEnabled(1);
        userMapper.insert(entity);
        return toAccountMap(entity, null);
    }

    @Transactional
    public Map<String, Object> updateAccount(Long id, AdminAccountUpdateRequest request) {
        UserEntity current = requireManagedUser(id);

        String requestAccountNo = trimToNull(request.getAccountNo());
        if (requestAccountNo != null && !requestAccountNo.equals(current.getAccountNo())) {
            throw new BizException(40001, "学号/工号创建后不可修改");
        }
        String requestRole = trimToNull(request.getRole());
        if (requestRole != null && !requestRole.equalsIgnoreCase(current.getRole())) {
            throw new BizException(40001, "角色创建后不可修改");
        }

        current.setRealName(normalizeRealName(request.getRealName()));
        current.setGender(normalizeGender(request.getGender()));
        current.setPhone(normalizePhone(request.getPhone()));
        String className = normalizeClassName(request.getClassName());
        validateClassName(current.getRole(), className);
        current.setClassName("STUDENT".equalsIgnoreCase(current.getRole()) ? className : null);

        userMapper.updateManagedFields(current);
        return toAccountMap(current, resolveDeleteBlockReason(current.getId()));
    }

    @Transactional
    public Map<String, Object> setEnabled(Long id, boolean enabled) {
        UserEntity current = requireManagedUser(id);
        int enabledValue = enabled ? 1 : 0;
        userMapper.updateEnabledById(id, enabledValue);
        current.setEnabled(enabledValue);
        return toAccountMap(current, resolveDeleteBlockReason(current.getId()));
    }

    @Transactional
    public Map<String, Object> resetPassword(Long id) {
        UserEntity current = requireManagedUser(id);
        userMapper.updatePasswordById(id, passwordEncoder.encode(defaultPassword));
        Map<String, Object> map = toAccountMap(current, resolveDeleteBlockReason(current.getId()));
        map.put("defaultPassword", defaultPassword);
        return map;
    }

    @Transactional
    public void deleteAccount(Long id) {
        UserEntity current = requireManagedUser(id);
        String reason = resolveDeleteBlockReason(current.getId());
        if (reason != null) {
            throw new BizException(40001, reason);
        }
        userMapper.deleteById(id);
    }

    public byte[] buildImportTemplate(String role) {
        String normalizedRole = normalizeRole(role, false);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("accounts");
            String[] headers = "STUDENT".equals(normalizedRole)
                    ? new String[]{"学号", "姓名", "性别", "联系电话", "班级"}
                    : new String[]{"工号", "姓名", "性别", "联系电话"};
            String[] sample = "STUDENT".equals(normalizedRole)
                    ? new String[]{"2022000100", "示例学生", "男", "13800000100", "2022级数据科学与大数据技术1班"}
                    : new String[]{"9000000100", "示例辅导员", "女", "13800000101"};
            var headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
                sheet.setColumnWidth(index, 20 * 256);
            }
            var sampleRow = sheet.createRow(1);
            for (int index = 0; index < sample.length; index++) {
                sampleRow.createCell(index).setCellValue(sample[index]);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BizException(50001, "导出模板失败: " + ex.getMessage());
        }
    }

    public Map<String, Object> previewImport(String role, MultipartFile file) {
        purgeExpiredPreviewSessions();
        String normalizedRole = normalizeRole(role, false);
        if (file == null || file.isEmpty()) {
            throw new BizException(40001, "请上传导入文件");
        }

        List<ImportRowData> validRows = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        Map<String, Integer> seenAccountNos = new HashMap<>();
        int totalRows = 0;

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.getBytes()))) {
            var sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BizException(40001, "导入文件内容为空");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row)) {
                    continue;
                }
                totalRows++;
                int displayRowNumber = rowIndex + 1;
                try {
                    ImportRowData rowData = parseImportRow(normalizedRole, row, displayRowNumber);
                    if (seenAccountNos.containsKey(rowData.accountNo)) {
                        throw new BizException(40001, "学号/工号与第 " + seenAccountNos.get(rowData.accountNo) + " 行重复");
                    }
                    seenAccountNos.put(rowData.accountNo, displayRowNumber);
                    if (userMapper.findAnyByAccountNo(rowData.accountNo) != null) {
                        throw new BizException(40001, "学号/工号已存在");
                    }
                    validRows.add(rowData);
                } catch (BizException ex) {
                    errors.add(buildImportError(displayRowNumber, row, ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            throw new BizException(40001, "导入文件解析失败，请上传有效的 xlsx 文件");
        }

        if (totalRows == 0) {
            throw new BizException(40001, "导入文件中没有可识别的数据行");
        }

        String previewToken = UUID.randomUUID().toString().replace("-", "");
        previewSessions.put(previewToken, new ImportPreviewSession(validRows, Instant.now().toEpochMilli()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("previewToken", previewToken);
        result.put("totalRows", totalRows);
        result.put("validRows", validRows.size());
        result.put("errorRows", errors.size());
        result.put("validItems", validRows.stream().map(this::toImportItemMap).toList());
        result.put("errors", errors);
        return result;
    }

    @Transactional
    public Map<String, Object> commitImport(String previewToken) {
        purgeExpiredPreviewSessions();
        String token = trimToNull(previewToken);
        if (token == null) {
            throw new BizException(40001, "预校验令牌不能为空");
        }

        ImportPreviewSession session = previewSessions.remove(token);
        if (session == null) {
            throw new BizException(40001, "预校验结果已失效，请重新上传文件");
        }

        List<Map<String, Object>> successItems = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();
        for (ImportRowData row : session.validRows) {
            try {
                ensureAccountNoAvailable(row.accountNo);
                UserEntity entity = new UserEntity();
                entity.setPasswordHash(passwordEncoder.encode(defaultPassword));
                entity.setRole(row.role);
                entity.setAccountNo(row.accountNo);
                entity.setRealName(row.realName);
                entity.setGender(row.gender);
                entity.setPhone(row.phone);
                entity.setClassName("STUDENT".equals(row.role) ? row.className : null);
                entity.setEnabled(1);
                userMapper.insert(entity);
                successItems.add(toAccountMap(entity, null));
            } catch (BizException ex) {
                Map<String, Object> failure = new LinkedHashMap<>();
                failure.put("rowNumber", row.rowNumber);
                failure.put("accountNo", row.accountNo);
                failure.put("message", ex.getMessage());
                failures.add(failure);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successItems.size());
        result.put("failedCount", failures.size());
        result.put("successItems", successItems);
        result.put("failures", failures);
        return result;
    }

    private ImportRowData parseImportRow(String role, Row row, int rowNumber) {
        String accountNo = normalizeAccountNo(readCell(row, 0));
        String realName = normalizeRealName(readCell(row, 1));
        String gender = normalizeGender(readCell(row, 2));
        String phone = normalizePhone(readCell(row, 3));
        String className = "STUDENT".equals(role) ? normalizeClassName(readCell(row, 4)) : null;
        validateClassName(role, className);
        return new ImportRowData(rowNumber, role, accountNo, realName, gender, phone, className);
    }

    private Map<String, Object> buildImportError(int rowNumber, Row row, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("rowNumber", rowNumber);
        map.put("accountNo", readCell(row, 0));
        map.put("message", message);
        return map;
    }

    private Map<String, Object> toImportItemMap(ImportRowData row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("rowNumber", row.rowNumber);
        map.put("role", row.role);
        map.put("accountNo", row.accountNo);
        map.put("realName", row.realName);
        map.put("gender", row.gender);
        map.put("phone", row.phone);
        map.put("className", row.className);
        return map;
    }

    private Map<String, Object> toAccountMap(UserEntity entity, String deleteBlockReason) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("role", entity.getRole());
        map.put("accountNo", entity.getAccountNo());
        map.put("realName", entity.getRealName());
        map.put("gender", entity.getGender());
        map.put("phone", entity.getPhone());
        map.put("className", entity.getClassName());
        map.put("enabled", entity.getEnabled());
        map.put("canDelete", deleteBlockReason == null);
        map.put("deleteBlockReason", deleteBlockReason);
        return map;
    }

    private UserEntity requireManagedUser(Long id) {
        if (id == null) {
            throw new BizException(40001, "账号不存在");
        }
        UserEntity user = userMapper.findById(id);
        if (user == null) {
            throw new BizException(40401, "账号不存在");
        }
        if (!MANAGED_ROLES.contains(normalizeRole(user.getRole(), false))) {
            throw new BizException(40001, "仅支持管理学生和辅导员账号");
        }
        return user;
    }

    private void ensureAccountNoAvailable(String accountNo) {
        if (userMapper.findAnyByAccountNo(accountNo) != null) {
            throw new BizException(40001, "学号/工号已存在");
        }
    }

    private void validateClassName(String role, String className) {
        if ("STUDENT".equals(role) && trimToNull(className) == null) {
            throw new BizException(40001, "学生班级不能为空");
        }
    }

    private String resolveDeleteBlockReason(Long userId) {
        if (userMapper.countSubmissionRefs(userId) > 0) {
            return "账号已有测评单数据，不能删除";
        }
        if (userMapper.countAttachmentUploadRefs(userId) > 0) {
            return "账号已有附件上传记录，不能删除";
        }
        if (userMapper.countNoticePublisherRefs(userId) > 0) {
            return "账号已有公告发布记录，不能删除";
        }
        if (userMapper.countCounselorScopeRefs(userId) > 0) {
            return "账号已配置班级权限，不能删除";
        }
        if (userMapper.countScopeAssignmentRefs(userId) > 0) {
            return "账号已有班级权限分配记录，不能删除";
        }
        if (userMapper.countFeedbackCreatorRefs(userId) > 0) {
            return "账号已有反馈记录，不能删除";
        }
        if (userMapper.countFeedbackHandlerRefs(userId) > 0) {
            return "账号已有反馈处理记录，不能删除";
        }
        return null;
    }

    private void purgeExpiredPreviewSessions() {
        long now = Instant.now().toEpochMilli();
        previewSessions.entrySet().removeIf(entry -> now - entry.getValue().createdAt > PREVIEW_TTL_MILLIS);
    }

    private String normalizeRole(String raw, boolean allowBlank) {
        String role = trimToNull(raw);
        if (role == null) {
            if (allowBlank) {
                return null;
            }
            throw new BizException(40001, "角色不能为空");
        }
        String normalized = role.toUpperCase(Locale.ROOT);
        if (!MANAGED_ROLES.contains(normalized)) {
            throw new BizException(40001, "角色仅支持学生或辅导员");
        }
        return normalized;
    }

    private String normalizeAccountNo(String raw) {
        String accountNo = trimToNull(raw);
        if (accountNo == null) {
            throw new BizException(40001, "学号/工号不能为空");
        }
        if (accountNo.length() < 4 || accountNo.length() > 32) {
            throw new BizException(40001, "学号/工号长度需在4-32位之间");
        }
        return accountNo;
    }

    private String normalizeRealName(String raw) {
        String realName = trimToNull(raw);
        if (realName == null) {
            throw new BizException(40001, "姓名不能为空");
        }
        if (realName.length() > 32) {
            throw new BizException(40001, "姓名长度不能超过32位");
        }
        return realName;
    }

    private String normalizeGender(String raw) {
        String gender = trimToNull(raw);
        if (gender == null) {
            throw new BizException(40001, "性别不能为空");
        }
        String upper = gender.toUpperCase(Locale.ROOT);
        if ("男".equals(gender) || "MALE".equals(upper) || "M".equals(upper)) {
            return "男";
        }
        if ("女".equals(gender) || "FEMALE".equals(upper) || "F".equals(upper)) {
            return "女";
        }
        throw new BizException(40001, "性别仅支持男/女");
    }

    private String normalizePhone(String raw) {
        String phone = trimToNull(raw);
        if (phone == null) {
            return null;
        }
        if (!phone.matches("^[0-9\\-]{7,20}$")) {
            throw new BizException(40001, "联系电话格式不正确");
        }
        return phone;
    }

    private String normalizeClassName(String raw) {
        String className = trimToNull(raw);
        if (className == null) {
            return null;
        }
        if (className.length() > 64) {
            throw new BizException(40001, "班级长度不能超过64位");
        }
        return className;
    }

    private String readCell(Row row, int columnIndex) {
        if (row == null) {
            return "";
        }
        var cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    private boolean isBlankRow(Row row) {
        if (row == null || row.getLastCellNum() < 0) {
            return true;
        }
        for (int index = 0; index < row.getLastCellNum(); index++) {
            if (!readCell(row, index).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static class ImportPreviewSession {
        private final List<ImportRowData> validRows;
        private final long createdAt;

        private ImportPreviewSession(List<ImportRowData> validRows, long createdAt) {
            this.validRows = List.copyOf(validRows);
            this.createdAt = createdAt;
        }
    }

    private static class ImportRowData {
        private final int rowNumber;
        private final String role;
        private final String accountNo;
        private final String realName;
        private final String gender;
        private final String phone;
        private final String className;

        private ImportRowData(int rowNumber, String role, String accountNo, String realName, String gender, String phone, String className) {
            this.rowNumber = rowNumber;
            this.role = role;
            this.accountNo = accountNo;
            this.realName = realName;
            this.gender = gender;
            this.phone = phone;
            this.className = className;
        }
    }
}
