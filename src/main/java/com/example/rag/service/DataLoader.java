package com.example.rag.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 启动时自动加载 docs/ 目录下的示例文档,把知识库建好,方便直接提问。
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final RagService ragService;
    private final VectorStore vectorStore;

    public DataLoader(RagService ragService, VectorStore vectorStore) {
        this.ragService = ragService;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        Path path = Path.of("docs", "员工手册.txt");
        if (!Files.exists(path)) {
            System.out.println("[DataLoader] 未找到 docs/员工手册.txt,跳过自动摄入");
            return;
        }
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            int chunks = ragService.ingest(text);
            System.out.println("[DataLoader] 已摄入 " + path + ", 共 " + chunks
                    + " 块, 向量库大小 = " + vectorStore.size());
        } catch (Exception e) {
            // 摄入失败(如 Embedding 接口暂时不可用)不阻断启动,仅打印日志
            System.err.println("[DataLoader] 自动摄入失败: " + e.getMessage());
        }
    }
}
