package com.example.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VectorStoreTest {

    @Test
    void cosineSimilarity_knownValues() {
        float[] a = {1, 2, 3};
        // 相同向量 -> 1.0
        assertEquals(1.0, VectorStore.cosineSimilarity(a, a), 1e-6);

        // 正交向量 -> 0.0
        assertEquals(0.0, VectorStore.cosineSimilarity(new float[]{1, 0, 0}, new float[]{0, 1, 0}), 1e-6);

        // 相反向量 -> -1.0
        assertEquals(-1.0, VectorStore.cosineSimilarity(new float[]{1, 0, 0}, new float[]{-1, 0, 0}), 1e-6);
    }

    @Test
    void cosineSimilarity_rejectsDimensionMismatch() {
        assertThrows(IllegalArgumentException.class,
                () -> VectorStore.cosineSimilarity(new float[]{1, 2}, new float[]{1, 2, 3}));
    }

    @Test
    void search_returnsTopKBySimilarity() {
        VectorStore store = new VectorStore();
        store.add(new Chunk(0, "猫"), new float[]{1, 0, 0});
        store.add(new Chunk(1, "狗"), new float[]{0, 1, 0});
        store.add(new Chunk(2, "猫咪"), new float[]{0.9f, 0.1f, 0});

        // 查询向量最接近"猫"
        List<VectorStore.Entry> results = store.search(new float[]{1, 0, 0}, 2);

        assertEquals(2, results.size());
        assertEquals(0, results.get(0).chunk().index(), "最相似的应该是 chunk0「猫」");
        assertEquals(2, results.get(1).chunk().index(), "次相似的是 chunk2「猫咪」");
    }
}
