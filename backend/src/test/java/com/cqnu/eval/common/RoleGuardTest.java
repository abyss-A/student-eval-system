package com.cqnu.eval.common;

import com.cqnu.eval.security.CurrentUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleGuardTest {

    @Test
    void requireRoleAllowsMatchedRole() {
        CurrentUser user = new CurrentUser(1L, "2022000001", "STUDENT");
        assertDoesNotThrow(() -> RoleGuard.requireRole(user, "student"));
    }

    @Test
    void requireRoleRejectsMismatchedRole() {
        CurrentUser user = new CurrentUser(1L, "2022000001", "STUDENT");
        BizException ex = assertThrows(BizException.class, () -> RoleGuard.requireRole(user, "ADMIN"));
        assertEquals(40301, ex.getCode());
    }

    @Test
    void requireAnyRoleAcceptsAnyMatchedRole() {
        CurrentUser user = new CurrentUser(2L, "9000000002", "COUNSELOR");
        assertDoesNotThrow(() -> RoleGuard.requireAnyRole(user, "ADMIN", "COUNSELOR"));
    }

    @Test
    void requireAnyRoleRejectsNullUser() {
        BizException ex = assertThrows(BizException.class, () -> RoleGuard.requireAnyRole(null, "ADMIN"));
        assertEquals(40301, ex.getCode());
    }
}
