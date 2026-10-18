package com.example.ebookrecommendsystem.ai;

import java.util.ArrayList;
import java.util.List;

/** Paragraph-aware chunks; oversized paragraphs use overlapping Unicode code-point windows. */
public class DocumentChunker {
    private final int size;
    private final int overlap;

    public DocumentChunker(int size, int overlap) {
        if (size < 80 || overlap < 0 || overlap >= size) throw new IllegalArgumentException("Invalid chunk limits");
        this.size = size;
        this.overlap = overlap;
    }

    public List<KnowledgeChunk> split(String id, String title, String content) {
        var result = new ArrayList<KnowledgeChunk>();
        for (String paragraph : content.replace("\r\n", "\n").split("\n\\s*\n")) {
            int[] points = paragraph.strip().codePoints().toArray();
            for (int start = 0; start < points.length;) {
                int length = Math.min(size, points.length - start);
                String text = new String(points, start, length);
                result.add(new KnowledgeChunk(id + "_" + (result.size() + 1), id, title, text));
                if (start + length == points.length) break;
                start += size - overlap;
            }
        }
        return List.copyOf(result);
    }
}
