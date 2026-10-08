package com.example.ebookrecommendsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AnswerVerifier {
    public record Citation(String sourceId, String title, String quote, String excerpt, double score, Integer bookId) {}
    public record Verified(String status, String answer, List<Citation> citations) {}
    private final ObjectMapper json;
    public AnswerVerifier(ObjectMapper json) { this.json = json; }

    public Verified verify(String raw, List<TfidfIndex.Hit> hits) {
        try {
            if (raw == null || raw.length() > 12000) return rejected();
            var node = json.readTree(raw);
            if (node == null || !node.isObject()) return rejected();
            String status = node.path("status").asText();
            if ("insufficient".equals(status)) return new Verified("INSUFFICIENT", "现有资料不足以回答这个问题。请补充项目资料，或请教师、项目维护者复核。", List.of());
            var answerNode = node.get("answer");
            var refs = node.path("citations");
            if (!"answered".equals(status) || answerNode == null || !answerNode.isTextual()
                    || answerNode.asText().isBlank() || answerNode.asText().length() > 2500
                    || !refs.isArray() || refs.isEmpty() || refs.size() > 3) return rejected();
            var allowed = new HashMap<String, TfidfIndex.Hit>();
            hits.forEach(hit -> allowed.put(hit.chunk().id(), hit));
            var citations = new ArrayList<Citation>();
            var seen = new HashSet<String>();
            for (var ref : refs) {
                String id = ref.path("sourceId").asText();
                String quote = ref.path("quote").asText().strip();
                var hit = allowed.get(id);
                if (hit == null || quote.length() < 6 || quote.length() > 480
                        || !hit.chunk().text().contains(quote) || !seen.add(id)) return rejected();
                citations.add(new Citation(id, hit.chunk().title(), quote, hit.chunk().text(), hit.score(), hit.chunk().bookId()));
            }
            // This checks source IDs and verbatim quotes, not semantic truth or entailment.
            return new Verified("ANSWERED", answerNode.asText().strip(), List.copyOf(citations));
        } catch (java.io.IOException | IllegalArgumentException e) { return rejected(); }
    }
    public Verified verifyGeneral(String raw) {
        try {
            if (raw == null || raw.length() > 12000) return rejected();
            var node = json.readTree(raw);
            if (node == null || !node.isObject() || !"general".equals(node.path("status").asText())
                    || !node.path("answer").isTextual() || node.path("answer").asText().isBlank()
                    || node.path("answer").asText().length() > 2500
                    || !node.path("citations").isArray() || !node.path("citations").isEmpty()) return rejected();
            return new Verified("GENERAL_KNOWLEDGE", node.path("answer").asText().strip(), List.of());
        } catch (java.io.IOException | IllegalArgumentException e) { return rejected(); }
    }
    private Verified rejected() {
        return new Verified("UNVERIFIED", "模型返回的格式或引用未通过核对，本次不展示该回答。请重试或查阅项目原文。", List.of());
    }
}
