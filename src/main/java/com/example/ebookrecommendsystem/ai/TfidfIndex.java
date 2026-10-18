package com.example.ebookrecommendsystem.ai;

import java.util.*;
import java.util.regex.Pattern;

/** Local sparse TF-IDF vectors, not neural embeddings. No network calls or user data ingestion. */
public class TfidfIndex {
    public record Hit(KnowledgeChunk chunk, double score) {}
    private static final Pattern TOKENS = Pattern.compile("[a-z0-9_]+|\\p{IsHan}+");
    private final List<KnowledgeChunk> chunks;
    private final Map<String, Double> idf;
    private final List<Map<String, Double>> vectors;

    public TfidfIndex(List<KnowledgeChunk> chunks) {
        this.chunks = List.copyOf(chunks);
        var counts = chunks.stream().map(c -> terms(c.title() + " " + c.text())).toList();
        var frequencies = new HashMap<String, Integer>();
        counts.forEach(count -> count.keySet().forEach(term -> frequencies.merge(term, 1, Integer::sum)));
        var weights = new HashMap<String, Double>();
        frequencies.forEach((term, count) -> weights.put(term, 1 + Math.log((chunks.size() + 1.0) / (count + 1.0))));
        this.idf = Map.copyOf(weights);
        this.vectors = counts.stream().map(this::vector).toList();
    }

    static Map<String, Integer> terms(String text) {
        var terms = new HashMap<String, Integer>();
        var matcher = TOKENS.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (Character.UnicodeScript.of(token.codePointAt(0)) == Character.UnicodeScript.HAN) {
                int[] points = token.codePoints().toArray();
                for (int i = 0; i + 1 < points.length; i++) terms.merge(new String(points, i, 2), 1, Integer::sum);
            } else terms.merge(token, 1, Integer::sum);
        }
        return terms;
    }

    private Map<String, Double> vector(Map<String, Integer> terms) {
        var vector = new HashMap<String, Double>();
        terms.forEach((term, count) -> {
            if (idf.containsKey(term)) vector.put(term, (1 + Math.log(count)) * idf.get(term));
        });
        double norm = Math.sqrt(vector.values().stream().mapToDouble(v -> v * v).sum());
        if (norm > 0) vector.replaceAll((term, weight) -> weight / norm);
        return Map.copyOf(vector);
    }

    public List<Hit> search(String question, int limit, double minimumScore) {
        if (limit < 1 || minimumScore < 0 || minimumScore > 1) throw new IllegalArgumentException("Invalid search limits");
        var query = vector(terms(question));
        if (query.isEmpty()) return List.of();
        var hits = new ArrayList<Hit>();
        for (int i = 0; i < chunks.size(); i++) {
            var doc = vectors.get(i);
            double score = query.entrySet().stream().mapToDouble(e -> e.getValue() * doc.getOrDefault(e.getKey(), 0.0)).sum();
            if (score >= minimumScore) hits.add(new Hit(chunks.get(i), score));
        }
        hits.sort(Comparator.comparingDouble(Hit::score).reversed().thenComparing(h -> h.chunk().id()));
        return List.copyOf(hits.subList(0, Math.min(limit, hits.size())));
    }
}
