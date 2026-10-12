package com.example.rag.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 内存版向量库:保存"文档块 + 它的向量",支持按余弦相似度检索 top-K。
 *
 * 教学版用内存 List 实现;进阶可替换为 Redis(RediSearch)、pgvector、Milvus 等真实向量库,
 * 接口保持不变。
 */
@Component
public class VectorStore {

    /** 一条记录:文档块 + 它对应的向量。 */
    public record Entry(Chunk chunk, float[] vector) {}

    private final List<Entry> entries = new ArrayList<>();

    /** 存入一个文档块及其向量。 */
    public void add(Chunk chunk, float[] vector) {
        entries.add(new Entry(chunk, vector));
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    /**
     * 检索与 queryVector 最相似的 topK 条记录,按相似度从高到低排序。
     */
    public List<Entry> search(float[] queryVector, int topK) {
        return entries.stream()
                .sorted(Comparator.comparingDouble(
                        (Entry e) -> cosineSimilarity(queryVector, e.vector())).reversed())
                .limit(topK)
                .toList();
    }

    /**
     * 余弦相似度:两个向量夹角的余弦,取值范围 [-1, 1],越大越相似。
     * 公式:cos = (A·B) / (|A| * |B|)
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不一致: " + a.length + " vs " + b.length);
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
