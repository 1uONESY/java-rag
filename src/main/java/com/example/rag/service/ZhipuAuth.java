package com.example.rag.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 智谱 AI 的 JWT 令牌生成器。
 *
 * 智谱的 API Key 格式是 <id>.<secret>,调用 /api/paas/v4/* 接口时
 * 需要用 secret 对 JWT 做 HMAC-SHA256 签名,不能直接把 key 塞进 Authorization。
 * 这里用 JDK 自带的 javax.crypto 手写签名,不引入第三方 JWT 库。
 */
public final class ZhipuAuth {

    private ZhipuAuth() {
    }

    /**
     * 根据智谱 API Key 生成 JWT 令牌。
     *
     * @param apiKey   智谱 key,格式 <id>.<secret>
     * @param ttlMillis 令牌有效期(毫秒),官方要求 ≤ 30 天
     */
    public static String generateToken(String apiKey, long ttlMillis) {
        String[] parts = apiKey.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("智谱 API Key 格式应为 <id>.<secret>");
        }
        String id = parts[0];
        String secret = parts[1];

        long now = System.currentTimeMillis();
        // header 与 payload 手动拼 JSON,避免依赖 Jackson 的对象序列化顺序
        String header = "{\"alg\":\"HS256\",\"sign_type\":\"SIGN\"}";
        String payload = "{\"api_key\":\"" + id + "\",\"exp\":" + (now + ttlMillis)
                + ",\"timestamp\":" + now + "}";

        String signingInput = base64Url(header) + "." + base64Url(payload);
        String signature = hmacSha256(secret, signingInput);
        return signingInput + "." + signature;
    }

    /** Base64 URL 安全编码(去掉末尾 = 号)。 */
    private static String base64Url(String s) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    /** HMAC-SHA256 签名,输出 Base64 URL 安全编码。 */
    private static String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new RuntimeException("生成智谱 JWT 失败", e);
        }
    }
}
