package com.example.rag.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文档切块器:把长文档切成适合检索的小块(chunk)。
 *
 * 切块策略:
 * 1. 先按段落(空行)切,尽量保留自然语义边界;
 * 2. 段落累积到 maxChars 就结算成一个 chunk;
 * 3. 单个段落超过 maxChars 时,用滑动窗口硬切,相邻窗口之间重叠 overlap 个字符,
 *    避免一句话被从中间截断。
 */
@Component
public class DocumentChunker {

    private static final int DEFAULT_MAX_CHARS = 500;
    private static final int DEFAULT_OVERLAP = 50;

    public List<Chunk> chunk(String text) {
        return chunk(text, DEFAULT_MAX_CHARS, DEFAULT_OVERLAP);
    }

    public List<Chunk> chunk(String text, int maxChars, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // 1. 按空行切段落
        List<String> paragraphs = Arrays.stream(text.split("\\n\\s*\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<Chunk> chunks = new ArrayList<>();
        int index = 0;
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            // 2. 单段落超长 -> 滑动窗口硬切
            if (para.length() > maxChars) {
                if (current.length() > 0) {
                    chunks.add(new Chunk(index++, current.toString()));
                    current = new StringBuilder();
                }
                List<Chunk> parts = splitLongParagraph(para, maxChars, overlap, index);
                chunks.addAll(parts);
                index += parts.size();
                continue;
            }

            // 3. 正常段落 -> 累积,超限则结算
            if (current.length() + para.length() + 1 <= maxChars) {
                if (current.length() > 0) current.append("\n");
                current.append(para);
            } else {
                chunks.add(new Chunk(index++, current.toString()));
                current = new StringBuilder(para);
            }
        }

        // 4. 收尾
        if (current.length() > 0) {
            chunks.add(new Chunk(index, current.toString()));
        }
        return chunks;
    }

    /** 超长段落按 maxChars 滑动窗口切,窗口之间重叠 overlap 个字符。 */
    private List<Chunk> splitLongParagraph(String para, int maxChars, int overlap, int startIndex) {
        List<Chunk> result = new ArrayList<>();
        int start = 0;
        while (start < para.length()) {
            int end = Math.min(start + maxChars, para.length());
            result.add(new Chunk(startIndex++, para.substring(start, end)));
            if (end >= para.length()) break;
            start = end - overlap;
        }
        return result;
    }
}
