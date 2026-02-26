package com.cqnu.eval.common;

import com.cqnu.eval.security.CurrentUser;

public class RoleGuard {

    private RoleGuard() {
    }

    public static void requireRole(CurrentUser user, String role) {
        if (user == null || !role.equalsIgnoreCase(user.getRole())) {
            throw new BizException(40301, "无权限访问");
        }
    }

    public static void requireAnyRole(CurrentUser user, String... roles) {
        if (user == null) {
            throw new BizException(40301, "无权限访问");
        }
        for (String role : roles) {
            if (role.equalsIgnoreCase(user.getRole())) {
                return;
            }
        }
        throw new BizException(40301, "无权限访问");
    }
}
