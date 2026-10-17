package com.example.ebookrecommendsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@Component
public class AnswerVerifier {
    private static final Logger log = LoggerFactory.getLogger(AnswerVerifier.class);
    private enum RejectionReason {
        NULL_OUTPUT, OUTPUT_TOO_LONG, NON_OBJECT_JSON, INVALID_STATUS,
        INVALID_ANSWER_TYPE, BLANK_ANSWER, ANSWER_TOO_LONG,
        INVALID_CITATIONS_TYPE, INVALID_CITATIONS_COUNT, UNKNOWN_SOURCE_ID,
        INVALID_QUOTE_LENGTH, NON_VERBATIM_QUOTE, DUPLICATE_SOURCE_ID,
        JSON_READ_FAILED, INVALID_ARGUMENT, INVALID_GENERAL_SUPPLEMENT
    }
    public record Citation(String sourceId, String title, String quote, String excerpt, double score, Integer bookId) {}
    public record Verified(String status, String answer, List<Citation> citations) {}
    private final ObjectMapper json;
    public AnswerVerifier(ObjectMapper json) { this.json = json; }

    public Verified verify(String raw, List<TfidfIndex.Hit> hits) {
        try {
            if (raw == null) return rejected(RejectionReason.NULL_OUTPUT);
            if (raw.length() > 12000) return rejected(RejectionReason.OUTPUT_TOO_LONG);
            var node = json.readTree(raw);
            if (node == null || !node.isObject()) return rejected(RejectionReason.NON_OBJECT_JSON);
            String status = node.path("status").asText();
            if ("insufficient".equals(status)) return new Verified("INSUFFICIENT", "现有资料不足以回答这个问题。请补充项目资料，或请教师、项目维护者复核。", List.of());
            var answerNode = node.get("answer");
            var refs = node.path("citations");
            if (!"answered".equals(status)) return rejected(RejectionReason.INVALID_STATUS);
            if (answerNode == null || !answerNode.isTextual()) return rejected(RejectionReason.INVALID_ANSWER_TYPE);
            if (answerNode.asText().isBlank()) return rejected(RejectionReason.BLANK_ANSWER);
            if (answerNode.asText().length() > 2500) return rejected(RejectionReason.ANSWER_TOO_LONG);
            if (!refs.isArray()) return rejected(RejectionReason.INVALID_CITATIONS_TYPE);
            if (refs.isEmpty() || refs.size() > 3) return rejected(RejectionReason.INVALID_CITATIONS_COUNT);
            var allowed = new HashMap<String, TfidfIndex.Hit>();
            hits.forEach(hit -> allowed.put(hit.chunk().id(), hit));
            var citations = new ArrayList<Citation>();
            var seen = new HashSet<String>();
            for (var ref : refs) {
                String id = ref.path("sourceId").asText();
                String quote = ref.path("quote").asText().strip();
                var hit = allowed.get(id);
                if (hit == null) return rejected(RejectionReason.UNKNOWN_SOURCE_ID);
                if (quote.length() < 6 || quote.length() > 480) return rejected(RejectionReason.INVALID_QUOTE_LENGTH);
                if (!hit.chunk().text().contains(quote)) return rejected(RejectionReason.NON_VERBATIM_QUOTE);
                if (!seen.add(id)) return rejected(RejectionReason.DUPLICATE_SOURCE_ID);
                citations.add(new Citation(id, hit.chunk().title(), quote, hit.chunk().text(), hit.score(), hit.chunk().bookId()));
            }
            // This checks source IDs and verbatim quotes, not semantic truth or entailment.
            return new Verified("ANSWERED", answerNode.asText().strip(), List.copyOf(citations));
        } catch (java.io.IOException e) { return rejected(RejectionReason.JSON_READ_FAILED); }
        catch (IllegalArgumentException e) { return rejected(RejectionReason.INVALID_ARGUMENT); }
    }
    public Verified verifyGeneral(String raw) {
        try {
            if (raw == null) return rejected(RejectionReason.NULL_OUTPUT);
            if (raw.length() > 12000) return rejected(RejectionReason.OUTPUT_TOO_LONG);
            var node = json.readTree(raw);
            if (node == null || !node.isObject()) return rejected(RejectionReason.NON_OBJECT_JSON);
            if (!"general".equals(node.path("status").asText())) return rejected(RejectionReason.INVALID_STATUS);
            if (!node.path("answer").isTextual()) return rejected(RejectionReason.INVALID_ANSWER_TYPE);
            if (node.path("answer").asText().isBlank()) return rejected(RejectionReason.BLANK_ANSWER);
            if (node.path("answer").asText().length() > 2500) return rejected(RejectionReason.ANSWER_TOO_LONG);
            if (!node.path("citations").isArray()) return rejected(RejectionReason.INVALID_CITATIONS_TYPE);
            if (!node.path("citations").isEmpty()) return rejected(RejectionReason.INVALID_CITATIONS_COUNT);
            if (containsSourceLink(node.path("answer").asText())) return rejected(RejectionReason.INVALID_GENERAL_SUPPLEMENT);
            return new Verified("GENERAL_KNOWLEDGE", node.path("answer").asText().strip(), List.of());
        } catch (java.io.IOException e) { return rejected(RejectionReason.JSON_READ_FAILED); }
        catch (IllegalArgumentException e) { return rejected(RejectionReason.INVALID_ARGUMENT); }
    }
    public record Supplement(boolean valid, String text) {}
    public Supplement verifySupplement(String raw, List<TfidfIndex.Hit> hits) {
        try {
            var node = json.readTree(raw);
            var part = node.get("generalSupplement");
            if (part == null || part.isNull()) return new Supplement(true, null);
            if (!part.isTextual() || part.asText().isBlank() || part.asText().length() > 2500
                    || containsSourceLink(part.asText())
                    || hits.stream().anyMatch(hit -> part.asText().contains(hit.chunk().id()))) return new Supplement(false, null);
            return new Supplement(true, part.asText().strip());
        } catch (java.io.IOException | IllegalArgumentException e) { return new Supplement(false, null); }
    }
    private static boolean containsSourceLink(String text) {
        return java.util.regex.Pattern.compile("(?i)(https?://|www\\.|/books(?:/|%2f)|book_[0-9]|sourceId|\\[[0-9]+\\]|站内资料编号)").matcher(text).find();
    }
    public Verified rejectPartition() { return rejected(RejectionReason.INVALID_GENERAL_SUPPLEMENT); }
    private Verified rejected(RejectionReason reason) {
        // Only fixed codes enter this log; never include model text, references or exceptions.
        log.warn("ANSWER_VERIFICATION_REJECTED reason={}", reason.name());
        return new Verified("UNVERIFIED", "模型返回的格式或引用未通过核对，本次不展示该回答。请重试或查阅项目原文。", List.of());
    }
}
