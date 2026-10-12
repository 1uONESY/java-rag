package com.example.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentChunkerTest {

    private final DocumentChunker chunker = new DocumentChunker();

    @Test
    void shouldSplitMultipleParagraphs() {
        String text = "第一段内容。\n\n第二段内容。\n\n第三段内容。";
        // maxChars=10 强制切成多块
        List<Chunk> chunks = chunker.chunk(text, 10, 0);

        assertTrue(chunks.size() >= 2, "应该切成多块");
        for (Chunk c : chunks) {
            assertTrue(c.text().length() <= 10, "chunk 超长: " + c.text());
        }
    }

    @Test
    void shouldReturnEmptyForBlankText() {
        assertTrue(chunker.chunk("").isEmpty());
        assertTrue(chunker.chunk("   \n\n  ").isEmpty());
    }

    @Test
    void shouldSplitLongParagraph() {
        // 500 个字符、没有空行
        String longText = "很长的段落".repeat(100);
        List<Chunk> chunks = chunker.chunk(longText, 100, 10);

        assertTrue(chunks.size() > 1, "超长段落应该被切成多块");
        for (Chunk c : chunks) {
            assertTrue(c.text().length() <= 100, "chunk 超长: " + c.text().length());
        }
    }

    @Test
    void chunksHaveIncreasingIndex() {
        String text = "a\n\nb\n\nc\n\nd\n\ne";
        List<Chunk> chunks = chunker.chunk(text, 3, 0);
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).index(), "chunk 编号应递增");
        }
    }
}
