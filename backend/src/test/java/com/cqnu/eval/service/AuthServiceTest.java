package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.UserMapper;
import com.cqnu.eval.model.dto.RegisterStudentRequest;
import com.cqnu.eval.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerStudentRequiresAdminManagedAccounts() {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setAccountNo("2022000001");
        request.setPassword("123456");
        request.setRealName("测试学生");
        request.setGender("男");
        request.setPhone("13800000001");
        request.setGradeClass("2022级数据科学与大数据技术1班");

        BizException ex = assertThrows(BizException.class, () -> authService.registerStudent(request));

        assertEquals(40301, ex.getCode());
        verify(userMapper, never()).insert(any());
    }
}
