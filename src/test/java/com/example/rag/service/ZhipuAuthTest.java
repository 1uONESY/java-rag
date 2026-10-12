package com.example.rag.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZhipuAuthTest {

    @Test
    void shouldGenerateThreePartJwt() {
        String token = ZhipuAuth.generateToken("id123.secret456", 3600_000L);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT 应为 header.payload.signature 三段");
        assertTrue(!parts[0].isEmpty() && !parts[1].isEmpty() && !parts[2].isEmpty());
    }

    @Test
    void headerAndPayloadAreCorrect() {
        String token = ZhipuAuth.generateToken("id123.secret456", 3600_000L);
        String[] parts = token.split("\\.");

        String header = decode(parts[0]);
        String payload = decode(parts[1]);

        assertTrue(header.contains("\"alg\":\"HS256\""), "header 应包含 HS256");
        assertTrue(header.contains("sign_type"));
        assertTrue(payload.contains("\"api_key\":\"id123\""), "payload 应包含 api_key");
        assertTrue(payload.contains("\"exp\""), "payload 应包含 exp");
    }

    @Test
    void shouldRejectMalformedKey() {
        assertThrows(IllegalArgumentException.class,
                () -> ZhipuAuth.generateToken("noDotHere", 1000L));
    }

    private String decode(String s) {
        return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8);
    }
}
