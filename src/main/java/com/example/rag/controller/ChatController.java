package com.example.rag.controller;

import com.example.rag.service.LlmService;
import com.example.rag.service.RagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final LlmService llmService;
    private final RagService ragService;

    public ChatController(LlmService llmService, RagService ragService) {
        this.llmService = llmService;
        this.ragService = ragService;
    }

    public record ChatRequest(String message) {}

    public record ChatResponse(String reply) {}

    public record IngestRequest(String text) {}

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    /** 直接对话(不带知识库),用于对比 RAG 效果。 */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest req) throws Exception {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "你是一个简洁、友好的中文助手。"),
                Map.of("role", "user", "content", req.message())
        );
        String reply = llmService.chat(messages);
        return new ChatResponse(reply);
    }

    /** 基于知识库回答(RAG)。 */
    @PostMapping("/ask")
    public RagService.Answer ask(@RequestBody ChatRequest req) throws Exception {
        return ragService.answer(req.message(), 3);
    }

    /** 手动摄入一篇文档。 */
    @PostMapping("/ingest")
    public Map<String, Integer> ingest(@RequestBody IngestRequest req) throws Exception {
        int chunks = ragService.ingest(req.text());
        return Map.of("chunks", chunks);
    }
}
