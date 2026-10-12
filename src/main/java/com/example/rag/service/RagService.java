package com.example.rag.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 编排服务:把「切块 -> 向量化 -> 检索 -> 生成」串起来。
 */
@Service
public class RagService {

    private final DocumentChunker chunker;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final LlmService llmService;

    public RagService(DocumentChunker chunker, EmbeddingService embeddingService,
                      VectorStore vectorStore, LlmService llmService) {
        this.chunker = chunker;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.llmService = llmService;
    }

    /** 回答 + 引用到的资料原文。 */
    public record Answer(String reply, List<String> sources) {}

    /**
     * 摄入一篇文档:切块 -> 每块向量化 -> 存入向量库。
     *
     * @return 切出来的块数
     */
    public int ingest(String documentText) throws Exception {
        List<Chunk> chunks = chunker.chunk(documentText);
        for (Chunk c : chunks) {
            vectorStore.add(c, embeddingService.embed(c.text()));
        }
        return chunks.size();
    }

    /**
     * 基于知识库回答问题。
     *
     * @param question 用户问题
     * @param topK     检索前 K 个最相关块
     */
    public Answer answer(String question, int topK) throws Exception {
        // 1. 把问题也转成向量
        float[] queryVector = embeddingService.embed(question);

        // 2. 检索最相关的 topK 块
        List<VectorStore.Entry> hits = vectorStore.search(queryVector, topK);
        List<String> sources = hits.stream().map(e -> e.chunk().text()).toList();

        // 3. 把检索到的资料拼进 prompt
        String context = hits.stream()
                .map(e -> "【资料" + e.chunk().index() + "】" + e.chunk().text())
                .collect(Collectors.joining("\n\n"));

        String system = "你是知识库问答助手。只根据下面提供的资料回答,资料里没有的就说不知道,不要编造。回答简洁,并在结尾标注引用了哪些资料。";
        String user = "资料:\n" + context + "\n\n问题:" + question;

        // 4. 调大模型生成回答
        String reply = llmService.chat(List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)));

        return new Answer(reply, sources);
    }
}
