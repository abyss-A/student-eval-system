package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.UserMapper;
import com.cqnu.eval.model.dto.AdminAccountCreateRequest;
import com.cqnu.eval.model.dto.AdminAccountUpdateRequest;
import com.cqnu.eval.model.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminAccountService adminAccountService;

    @BeforeEach
    void setUp() {
        adminAccountService = new AdminAccountService(userMapper, passwordEncoder);
        ReflectionTestUtils.setField(adminAccountService, "defaultPassword", "123456");
    }

    @Test
    void createStudentAccountUsesConfiguredDefaultPassword() {
        when(userMapper.findAnyByAccountNo("2022000099")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("ENC_123456");
        doAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(88L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));

        AdminAccountCreateRequest request = new AdminAccountCreateRequest();
        request.setRole("STUDENT");
        request.setAccountNo("2022000099");
        request.setRealName("新学生");
        request.setGender("男");
        request.setPhone("");
        request.setClassName("2022级数据科学与大数据技术9班");

        Map<String, Object> result = adminAccountService.createAccount(request);

        assertEquals(88L, ((Number) result.get("id")).longValue());
        assertEquals("STUDENT", result.get("role"));
        assertEquals("2022000099", result.get("accountNo"));
        assertEquals(1, ((Number) result.get("enabled")).intValue());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).insert(captor.capture());
        UserEntity inserted = captor.getValue();
        assertEquals("ENC_123456", inserted.getPasswordHash());
        assertEquals("STUDENT", inserted.getRole());
        assertEquals("2022000099", inserted.getAccountNo());
        assertEquals("2022级数据科学与大数据技术9班", inserted.getClassName());
        assertEquals(1, inserted.getEnabled());
    }

    @Test
    void updateAccountRejectsAccountNoChange() {
        UserEntity current = managedUser(11L, "STUDENT", "2022000001");
        when(userMapper.findById(11L)).thenReturn(current);

        AdminAccountUpdateRequest request = new AdminAccountUpdateRequest();
        request.setAccountNo("2022009999");
        request.setRole("STUDENT");
        request.setRealName("改名");
        request.setGender("女");
        request.setClassName("2022级信息与计算科学1班");

        BizException ex = assertThrows(BizException.class, () -> adminAccountService.updateAccount(11L, request));

        assertEquals(40001, ex.getCode());
        verify(userMapper, never()).updateManagedFields(any(UserEntity.class));
    }

    @Test
    void setEnabledUpdatesFlag() {
        UserEntity current = managedUser(12L, "COUNSELOR", "9000000099");
        current.setEnabled(1);
        when(userMapper.findById(12L)).thenReturn(current);
        when(userMapper.updateEnabledById(12L, 0)).thenReturn(1);

        Map<String, Object> result = adminAccountService.setEnabled(12L, false);

        assertEquals(0, ((Number) result.get("enabled")).intValue());
        verify(userMapper).updateEnabledById(12L, 0);
    }

    @Test
    void resetPasswordUsesConfiguredDefaultPassword() {
        when(userMapper.findById(13L)).thenReturn(managedUser(13L, "COUNSELOR", "9000000013"));
        when(passwordEncoder.encode("123456")).thenReturn("ENC_RESET");
        when(userMapper.updatePasswordById(13L, "ENC_RESET")).thenReturn(1);

        Map<String, Object> result = adminAccountService.resetPassword(13L);

        assertEquals("9000000013", result.get("accountNo"));
        assertEquals("123456", result.get("defaultPassword"));
        verify(userMapper).updatePasswordById(13L, "ENC_RESET");
    }

    @Test
    void deleteAccountRejectsReferencedUser() {
        when(userMapper.findById(14L)).thenReturn(managedUser(14L, "COUNSELOR", "9000000014"));
        when(userMapper.countSubmissionRefs(14L)).thenReturn(0L);
        when(userMapper.countAttachmentUploadRefs(14L)).thenReturn(0L);
        when(userMapper.countNoticePublisherRefs(14L)).thenReturn(0L);
        when(userMapper.countCounselorScopeRefs(14L)).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> adminAccountService.deleteAccount(14L));

        assertEquals(40001, ex.getCode());
        verify(userMapper, never()).deleteById(14L);
    }

    @Test
    void deleteAccountDeletesUnreferencedUser() {
        when(userMapper.findById(15L)).thenReturn(managedUser(15L, "COUNSELOR", "9000000015"));
        when(userMapper.countSubmissionRefs(15L)).thenReturn(0L);
        when(userMapper.countAttachmentUploadRefs(15L)).thenReturn(0L);
        when(userMapper.countNoticePublisherRefs(15L)).thenReturn(0L);
        when(userMapper.countCounselorScopeRefs(15L)).thenReturn(0L);
        when(userMapper.countScopeAssignmentRefs(15L)).thenReturn(0L);
        when(userMapper.countFeedbackCreatorRefs(15L)).thenReturn(0L);
        when(userMapper.countFeedbackHandlerRefs(15L)).thenReturn(0L);
        when(userMapper.deleteById(15L)).thenReturn(1);

        adminAccountService.deleteAccount(15L);

        verify(userMapper).deleteById(15L);
    }

    @Test
    void previewImportBuildsValidRowsAndErrors() throws Exception {
        when(userMapper.findAnyByAccountNo("2022000002")).thenReturn(null);
        when(userMapper.findAnyByAccountNo("2022000001")).thenReturn(managedUser(1L, "STUDENT", "2022000001"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes(new String[]{"学号", "姓名", "性别", "联系电话", "班级"}, new String[][]{
                        {"2022000002", "学生二", "男", "13800000002", "2022级数据科学与大数据技术2班"},
                        {"2022000001", "重复学生", "女", "13800000003", "2022级信息与计算科学1班"},
                        {"2022000004", "缺班级学生", "男", "13800000004", ""}
                })
        );

        Map<String, Object> preview = adminAccountService.previewImport("STUDENT", file);

        assertEquals(3, ((Number) preview.get("totalRows")).intValue());
        assertEquals(1, ((Number) preview.get("validRows")).intValue());
        assertEquals(2, ((Number) preview.get("errorRows")).intValue());
        assertFalse(String.valueOf(preview.get("previewToken")).isBlank());

        List<?> validItems = (List<?>) preview.get("validItems");
        List<?> errors = (List<?>) preview.get("errors");
        assertEquals(1, validItems.size());
        assertEquals(2, errors.size());
    }

    @Test
    void commitImportImportsValidRowsAndSkipsNewConflicts() throws Exception {
        when(userMapper.findAnyByAccountNo("2022000101")).thenReturn(null, null);
        when(userMapper.findAnyByAccountNo("2022000102")).thenReturn(null, managedUser(101L, "STUDENT", "2022000102"));
        when(passwordEncoder.encode("123456")).thenReturn("ENC_IMPORT");
        doAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(300L + System.nanoTime() % 1000);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes(new String[]{"学号", "姓名", "性别", "联系电话", "班级"}, new String[][]{
                        {"2022000101", "可导入学生", "男", "13800000101", "2022级数据科学与大数据技术1班"},
                        {"2022000102", "并发冲突学生", "女", "13800000102", "2022级数据科学与大数据技术2班"}
                })
        );

        Map<String, Object> preview = adminAccountService.previewImport("STUDENT", file);
        String previewToken = String.valueOf(preview.get("previewToken"));

        Map<String, Object> result = adminAccountService.commitImport(previewToken);

        assertEquals(1, ((Number) result.get("successCount")).intValue());
        assertEquals(1, ((Number) result.get("failedCount")).intValue());
        List<?> failures = (List<?>) result.get("failures");
        assertEquals(1, failures.size());
        verify(userMapper, times(1)).insert(any(UserEntity.class));
    }

    private UserEntity managedUser(Long id, String role, String accountNo) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setRole(role);
        entity.setAccountNo(accountNo);
        entity.setRealName("测试用户");
        entity.setGender("男");
        entity.setPhone("13800000000");
        entity.setClassName("2022级数据科学与大数据技术1班");
        entity.setEnabled(1);
        return entity;
    }

    private byte[] workbookBytes(String[] headers, String[][] rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("accounts");
            var headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                var row = sheet.createRow(rowIndex + 1);
                for (int col = 0; col < rows[rowIndex].length; col++) {
                    row.createCell(col).setCellValue(rows[rowIndex][col]);
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
