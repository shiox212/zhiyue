package com.example.ebookrecommendsystem.ai;

import com.example.ebookrecommendsystem.mapper.BookMapper;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Comparator;

/** Query only relevant public metadata, never shelves, reviews or user records. */
@Component
public class BookKnowledge {
    private final BookMapper books;
    public BookKnowledge(BookMapper books) { this.books = books; }

    public List<TfidfIndex.Hit> retrieve(String question) {
        if (question.codePoints().noneMatch(Character::isLetterOrDigit)) return List.of();
        var keywords = TfidfIndex.terms(question).entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(java.util.Map.Entry.comparingByKey()))
                .map(java.util.Map.Entry::getKey).limit(32).toList();
        var chunks = new ArrayList<KnowledgeChunk>();
        var priorities = new HashMap<Integer, Integer>();
        for (var book : books.findAssistantCandidates(keywords, question)) {
            if (book.getId() == null || book.getId() <= 0 || book.getTitle() == null || book.getTitle().isBlank()) continue;
            String id = "book_" + book.getId();
            // Full title/author occurrences outrank incidental bigram matches in descriptions.
            priorities.put(book.getId(), (completeMatch(question, book.getTitle()) ? 2 : 0)
                    + (completeMatch(question, book.getAuthor()) ? 1 : 0));
            String text = "图书ID：" + book.getId() + "\n书名：" + bounded(book.getTitle(), 200)
                    + "\n作者：" + bounded(book.getAuthor(), 50) + "\n分类：" + bounded(book.getCategory(), 30)
                    + "\n简介：" + bounded(book.getDescription(), 2000);
            new DocumentChunker(480, 60).split(id, bounded(book.getTitle(), 200), text)
                    .forEach(chunk -> chunks.add(new KnowledgeChunk(chunk.id(), id, chunk.title(), chunk.text(), book.getId())));
        }
        if (chunks.isEmpty()) return List.of();
        var scores = new HashMap<String, Double>();
        new TfidfIndex(chunks).search(question, chunks.size(), 0)
                .forEach(hit -> scores.put(hit.chunk().id(), hit.score()));
        // Punctuated titles can have no bigrams; exact public metadata remains evidence.
        return chunks.stream()
                .map(chunk -> new TfidfIndex.Hit(chunk, scores.getOrDefault(chunk.id(), 0.0)))
                .filter(hit -> hit.score() >= 0.13
                        || priorities.getOrDefault(hit.chunk().bookId(), 0) > 0)
                .map(hit -> new TfidfIndex.Hit(hit.chunk(), hit.score() + priorities.getOrDefault(hit.chunk().bookId(), 0)))
                .sorted(Comparator.comparingDouble(TfidfIndex.Hit::score).reversed().thenComparing(hit -> hit.chunk().id()))
                .limit(3).toList();
    }
    private static boolean completeMatch(String question, String value) {
        return value != null && !value.isBlank()
                && question.toLowerCase(Locale.ROOT).contains(value.strip().toLowerCase(Locale.ROOT));
    }
    private static String bounded(String value, int limit) {
        if (value == null || value.isBlank()) return "未收录";
        return value.substring(0, Math.min(value.length(), limit));
    }
}
