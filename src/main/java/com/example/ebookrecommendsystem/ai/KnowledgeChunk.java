package com.example.ebookrecommendsystem.ai;

public record KnowledgeChunk(String id, String documentId, String title, String text, Integer bookId) {
    public KnowledgeChunk(String id, String documentId, String title, String text) {
        this(id, documentId, title, text, null);
    }
}
