package com.example.ebookrecommendsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AnswerVerifierTest {
    private static final String SENTINEL = "SENSITIVE_FIXTURE_7e2b\nFORGED_LOG_LINE";
    private record RejectedFixture(String raw, String reason) {}

    private String output(String status, Object answer, Object citations) throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of("status", status, "answer", answer, "citations", citations));
    }
    private Map<String, String> ref(String id, String quote) {
        return Map.of("sourceId", id, "quote", quote);
    }
    private void withCapturedLogs(java.util.function.Consumer<ListAppender<ILoggingEvent>> check) {
        Logger logger = (Logger) LoggerFactory.getLogger(AnswerVerifier.class);
        var appender = new ListAppender<ILoggingEvent>();
        Level originalLevel = logger.getLevel();
        boolean originalAdditive = logger.isAdditive();
        appender.start();
        logger.setLevel(Level.WARN);
        logger.setAdditive(false);
        logger.addAppender(appender);
        try {
            check.accept(appender);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
            logger.setAdditive(originalAdditive);
            appender.stop();
        }
    }
    private void assertOnlyFixedReason(ListAppender<ILoggingEvent> logs, String reason) {
        assertEquals(1, logs.list.size());
        var event = logs.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertEquals("ANSWER_VERIFICATION_REJECTED reason={}", event.getMessage());
        assertArrayEquals(new Object[] {reason}, event.getArgumentArray());
        assertEquals("ANSWER_VERIFICATION_REJECTED reason=" + reason, event.getFormattedMessage());
        assertTrue(reason.matches("[A-Z_]+"));
        assertNull(event.getThrowableProxy());
        assertFalse(event.getFormattedMessage().contains("SENSITIVE_FIXTURE"));
        assertFalse(event.getFormattedMessage().contains("FORGED_LOG_LINE"));
    }
    @Test void libraryRejectionsLogOnlyFixedReasonsAndNeverUntrustedContent() throws Exception {
        var validRefs = List.of(ref("auth_1", "访问令牌默认有效期900秒。"));
        var fixtures = List.of(
                new RejectedFixture(null, "NULL_OUTPUT"),
                new RejectedFixture(SENTINEL.repeat(400), "OUTPUT_TOO_LONG"),
                new RejectedFixture(SENTINEL, "JSON_READ_FAILED"),
                new RejectedFixture("[]", "NON_OBJECT_JSON"),
                new RejectedFixture("null", "NON_OBJECT_JSON"),
                new RejectedFixture("", "NON_OBJECT_JSON"),
                new RejectedFixture(output(SENTINEL, SENTINEL, validRefs), "INVALID_STATUS"),
                new RejectedFixture(output("answered", Map.of("private", SENTINEL), validRefs), "INVALID_ANSWER_TYPE"),
                new RejectedFixture("{\"status\":\"answered\"}", "INVALID_ANSWER_TYPE"),
                new RejectedFixture(output("answered", " \n", validRefs), "BLANK_ANSWER"),
                new RejectedFixture(output("answered", SENTINEL + "x".repeat(2501), validRefs), "ANSWER_TOO_LONG"),
                new RejectedFixture(output("answered", SENTINEL, SENTINEL), "INVALID_CITATIONS_TYPE"),
                new RejectedFixture(output("answered", SENTINEL, List.of()), "INVALID_CITATIONS_COUNT"),
                new RejectedFixture(output("answered", SENTINEL, java.util.Collections.nCopies(4, validRefs.get(0))), "INVALID_CITATIONS_COUNT"),
                new RejectedFixture(output("answered", SENTINEL, List.of(ref(SENTINEL, SENTINEL))), "UNKNOWN_SOURCE_ID"),
                new RejectedFixture(output("answered", SENTINEL, List.of(ref("auth_1", "短引用"))), "INVALID_QUOTE_LENGTH"),
                new RejectedFixture(output("answered", SENTINEL, List.of(ref("auth_1", SENTINEL + "x".repeat(481)))), "INVALID_QUOTE_LENGTH"),
                new RejectedFixture(output("answered", SENTINEL, List.of(ref("auth_1", SENTINEL))), "NON_VERBATIM_QUOTE"),
                new RejectedFixture(output("answered", SENTINEL, java.util.Collections.nCopies(2, validRefs.get(0))), "DUPLICATE_SOURCE_ID"));
        withCapturedLogs(logs -> {
            for (var fixture : fixtures) {
                logs.list.clear();
                var result = verifier.verify(fixture.raw(), hits);
                assertEquals("UNVERIFIED", result.status());
                assertTrue(result.citations().isEmpty());
                assertFalse(result.answer().contains("SENSITIVE_FIXTURE"));
                assertOnlyFixedReason(logs, fixture.reason());
            }
        });
    }
    @Test void generalRejectionsUseTheSameSafeDiagnosticContract() throws Exception {
        var fixtures = List.of(
                new RejectedFixture(null, "NULL_OUTPUT"),
                new RejectedFixture(SENTINEL.repeat(400), "OUTPUT_TOO_LONG"),
                new RejectedFixture(SENTINEL, "JSON_READ_FAILED"),
                new RejectedFixture("null", "NON_OBJECT_JSON"),
                new RejectedFixture(output(SENTINEL, SENTINEL, List.of()), "INVALID_STATUS"),
                new RejectedFixture(output("general", List.of(SENTINEL), List.of()), "INVALID_ANSWER_TYPE"),
                new RejectedFixture(output("general", " \n", List.of()), "BLANK_ANSWER"),
                new RejectedFixture(output("general", SENTINEL + "x".repeat(2501), List.of()), "ANSWER_TOO_LONG"),
                new RejectedFixture(output("general", SENTINEL, SENTINEL), "INVALID_CITATIONS_TYPE"),
                new RejectedFixture(output("general", SENTINEL, List.of(ref(SENTINEL, SENTINEL))), "INVALID_CITATIONS_COUNT"));
        withCapturedLogs(logs -> {
            for (var fixture : fixtures) {
                logs.list.clear();
                assertEquals("UNVERIFIED", verifier.verifyGeneral(fixture.raw()).status());
                assertOnlyFixedReason(logs, fixture.reason());
            }
        });
    }
    @Test void caughtExceptionsNeverLogTheirMessageOrThrowable() throws Exception {
        for (Exception failure : List.of(new JsonParseException(null, SENTINEL), new IllegalArgumentException(SENTINEL))) {
            var mapper = mock(ObjectMapper.class);
            when(mapper.readTree(anyString())).thenThrow(failure);
            var failingVerifier = new AnswerVerifier(mapper);
            String reason = failure instanceof IOException ? "JSON_READ_FAILED" : "INVALID_ARGUMENT";
            withCapturedLogs(logs -> {
                assertEquals("UNVERIFIED", failingVerifier.verify(SENTINEL, hits).status());
                assertOnlyFixedReason(logs, reason);
                logs.list.clear();
                assertEquals("UNVERIFIED", failingVerifier.verifyGeneral(SENTINEL).status());
                assertOnlyFixedReason(logs, reason);
            });
        }
    }
    @Test void acceptedAndInsufficientResultsDoNotLogRejections() throws Exception {
        String valid = output("answered", SENTINEL, List.of(ref("auth_1", "访问令牌默认有效期900秒。")));
        String general = output("general", SENTINEL, List.of());
        String insufficient = output("insufficient", SENTINEL, SENTINEL);
        withCapturedLogs(logs -> {
            assertEquals("ANSWERED", verifier.verify(valid, hits).status());
            assertEquals("GENERAL_KNOWLEDGE", verifier.verifyGeneral(general).status());
            assertEquals("INSUFFICIENT", verifier.verify(insufficient, hits).status());
            assertTrue(logs.list.isEmpty());
        });
    }
    @Test void originalLengthAndQuoteStripBoundariesRemainAccepted() throws Exception {
        var boundaryHits = List.of(new TfidfIndex.Hit(new KnowledgeChunk("boundary", "fixture", "fixture", "x".repeat(480)), .8));
        for (int quoteLength : List.of(6, 480)) {
            String valid = output("answered", "a".repeat(2500), List.of(ref("boundary", " \n" + "x".repeat(quoteLength) + " \n")));
            assertEquals("ANSWERED", verifier.verify(valid + " ".repeat(12000 - valid.length()), boundaryHits).status());
        }
        assertEquals("GENERAL_KNOWLEDGE", verifier.verifyGeneral(output("general", "a".repeat(2500), List.of())).status());
    }
    private final AnswerVerifier verifier = new AnswerVerifier(new ObjectMapper());
    private final List<TfidfIndex.Hit> hits = List.of(new TfidfIndex.Hit(
            new KnowledgeChunk("auth_1", "auth", "登录说明", "访问令牌默认有效期900秒。刷新凭据默认有效期7天。"), .8));
    @Test void validVerbatimQuoteUsesServerOwnedTitleAndExcerpt() {
        var result = verifier.verify("{\"status\":\"answered\",\"answer\":\"访问令牌900秒\",\"citations\":[{\"sourceId\":\"auth_1\",\"quote\":\"访问令牌默认有效期900秒。\",\"title\":\"模型编造标题\"}]}", hits);
        assertEquals("ANSWERED", result.status());
        assertEquals("登录说明", result.citations().get(0).title());
        assertEquals(hits.get(0).chunk().text(), result.citations().get(0).excerpt());
    }
    @Test void fabricatedSourceOrQuoteIsNotDisplayed() {
        for (String ref : List.of("{\"sourceId\":\"unknown\",\"quote\":\"访问令牌默认有效期900秒。\"}",
                "{\"sourceId\":\"auth_1\",\"quote\":\"访问令牌默认有效期365天。\"}")) {
            var result = verifier.verify("{\"status\":\"answered\",\"answer\":\"未经验证的错误回答\",\"citations\":[" + ref + "]}", hits);
            assertEquals("UNVERIFIED", result.status());
            assertFalse(result.answer().contains("未经验证的错误回答"));
        }
    }
    @Test void malformedEmptyAndOversizedOutputsAreRejected() {
        for (String raw : List.of("not json", "[]", "null", "{\"status\":\"answered\",\"answer\":\"猜测\",\"citations\":[]}", "x".repeat(12001))) {
            assertEquals("UNVERIFIED", verifier.verify(raw, hits).status());
        }
    }
    @Test void insufficientAnswerDoesNotPassThroughModelGuesses() {
        var result = verifier.verify("{\"status\":\"insufficient\",\"answer\":\"但是我猜密码是123456\"}", hits);
        assertEquals("INSUFFICIENT", result.status());
        assertFalse(result.answer().contains("123456"));
    }
    @Test void validQuoteDoesNotProveTheAnswerIsTrue() {
        // Deliberately documents the semantic limitation instead of claiming full hallucination protection.
        var result = verifier.verify("{\"status\":\"answered\",\"answer\":\"令牌有效期365天\",\"citations\":[{\"sourceId\":\"auth_1\",\"quote\":\"访问令牌默认有效期900秒。\"}]}", hits);
        assertEquals("ANSWERED", result.status());
    }
}
