package com.example.rag.service;

/**
 * 文档切块后的一小块文本。
 * index 是块编号,text 是块内容。
 */
public record Chunk(int index, String text) {
}
