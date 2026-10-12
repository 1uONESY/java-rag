package com.example.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 调用 Embedding 接口,把一段文本转成向量(float[])。
 *
 * 通过配置 provider 支持不同提供商:
 * - zhipu(智谱): 需要 JWT 签名,模型 embedding-2 / embedding-3
 * - openai(OpenAI 兼容,如硅基流动): 直接 Bearer 传 key
 */
@Service
public class EmbeddingService {

    private static final long JWT_TTL_MILLIS = 60 * 60 * 1000L; // 1 小时

    private final String apiKey;
    private final String provider;
    private final String endpoint;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public EmbeddingService(@Value("${embedding.api-key}") String apiKey,
                            @Value("${embedding.endpoint}") String endpoint,
                            @Value("${embedding.model}") String model,
                            @Value("${embedding.provider:openai}") String provider) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
        this.provider = provider;
    }

    /**
     * 把文本转成向量。
     *
     * @param text 任意文本
     * @return 向量,每个元素是 float
     */
    public float[] embed(String text) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("input", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", buildAuthHeader())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Embedding 接口返回 " + resp.statusCode() + ": " + resp.body());
        }

        JsonNode root = mapper.readTree(resp.body());
        JsonNode embedding = root.path("data").get(0).path("embedding");
        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = (float) embedding.get(i).asDouble();
        }
        return vector;
    }

    /** 按 provider 生成 Authorization 头。智谱需要 JWT,其余直接 Bearer。 */
    private String buildAuthHeader() {
        if ("zhipu".equalsIgnoreCase(provider)) {
            return "Bearer " + ZhipuAuth.generateToken(apiKey, JWT_TTL_MILLIS);
        }
        return "Bearer " + apiKey;
    }
}
