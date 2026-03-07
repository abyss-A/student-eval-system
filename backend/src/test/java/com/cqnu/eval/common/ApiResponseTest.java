package com.cqnu.eval.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {

    @Test
    void okResponseContainsData() {
        ApiResponse<String> response = ApiResponse.ok("hello");
        assertEquals(0, response.getCode());
        assertEquals("ok", response.getMessage());
        assertEquals("hello", response.getData());
    }

    @Test
    void failResponseContainsCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.fail(40001, "bad request");
        assertEquals(40001, response.getCode());
        assertEquals("bad request", response.getMessage());
        assertNull(response.getData());
    }
}
