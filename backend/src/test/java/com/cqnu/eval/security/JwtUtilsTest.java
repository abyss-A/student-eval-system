package com.cqnu.eval.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtUtilsTest {

    @Test
    void createAndParseTokenRoundTrip() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", "student-eval-backend-secret-key-for-tests");
        ReflectionTestUtils.setField(jwtUtils, "expireHours", 2);
        jwtUtils.init();

        String token = jwtUtils.createToken(99L, "9000000001", "ADMIN");
        assertNotNull(token);

        Claims claims = jwtUtils.parse(token);
        assertEquals("9000000001", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals(99L, claims.get("uid", Number.class).longValue());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void shortSecretIsAutoPadded() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", "short-secret");
        ReflectionTestUtils.setField(jwtUtils, "expireHours", 1);
        jwtUtils.init();

        String token = jwtUtils.createToken(1L, "2022000001", "STUDENT");
        Claims claims = jwtUtils.parse(token);

        assertEquals("2022000001", claims.getSubject());
        assertEquals("STUDENT", claims.get("role", String.class));
    }
}
