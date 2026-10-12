package com.example.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 封装对大模型(DeepSeek)的 HTTP 调用。
 * 这里用 JDK 自带的 HttpClient,不引入额外依赖,便于理解底层原理。
 */
@Service
public class LlmService {

    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public LlmService(@Value("${llm.api-key}") String apiKey,
                      @Value("${llm.base-url}") String baseUrl,
                      @Value("${llm.model}") String model) {
        this.apiKey = apiKey;
        // 拼接成 chat/completions 接口地址
        this.endpoint = baseUrl.endsWith("/") ? baseUrl + "v1/chat/completions"
                                              : baseUrl + "/v1/chat/completions";
        this.model = model;
    }

    /**
     * 调用大模型。
     *
     * @param messages 消息列表,每项包含 role(system/user/assistant) 和 content
     * @return 模型回复的文本
     */
    public String chat(List<Map<String, String>> messages) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.7);
        body.put("stream", false);

        ArrayNode msgs = body.putArray("messages");
        for (Map<String, String> m : messages) {
            ObjectNode msg = msgs.addObject();
            msg.put("role", m.get("role"));
            msg.put("content", m.get("content"));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("上游接口返回 " + resp.statusCode() + ": " + resp.body());
        }

        JsonNode root = mapper.readTree(resp.body());
        return root.path("choices").get(0).path("message").path("content").asText();
    }
}
