package com.example.ebookrecommendsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;

@Component
public class KnowledgeBase {
    public record Document(String id, String title, String content) {}
    private final List<Document> documents;
    private final List<KnowledgeChunk> chunks;
    private final TfidfIndex index;

    public KnowledgeBase(ObjectMapper json) throws IOException {
        // Explicitly curated, shareable project notes only. Never scan directories or .env files.
        try (var input = new ClassPathResource("knowledge/zhiyue-guide.json").getInputStream()) {
            documents = List.of(json.readValue(input, Document[].class));
        }
        if (documents.isEmpty() || documents.size() > 20) throw new IllegalStateException("Invalid knowledge document count");
        var ids = new HashSet<String>();
        var all = new ArrayList<KnowledgeChunk>();
        var chunker = new DocumentChunker(480, 60);
        for (var doc : documents) {
            if (doc.id() == null || !doc.id().matches("[a-z0-9_-]{1,40}") || !ids.add(doc.id())
                    || doc.title() == null || doc.title().isBlank() || doc.content() == null
                    || doc.content().isBlank() || doc.content().length() > 12000) {
                throw new IllegalStateException("Invalid curated knowledge document");
            }
            all.addAll(chunker.split(doc.id(), doc.title(), doc.content()));
        }
        chunks = List.copyOf(all);
        index = new TfidfIndex(chunks);
    }
    public List<TfidfIndex.Hit> retrieve(String question) { return index.search(question, 3, 0.13); }
    public List<KnowledgeChunk> chunks() { return chunks; }
    public int documentCount() { return documents.size(); }
}
