package com.cqnu.eval.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserContextTest {

    @Test
    void setGetAndClearCurrentUser() {
        CurrentUser user = new CurrentUser(10L, "2022000001", "STUDENT");

        UserContext.set(user);
        assertEquals("2022000001", UserContext.get().getAccountNo());

        UserContext.clear();
        assertNull(UserContext.get());
    }
}
