package com.example.ebookrecommendsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AnswerVerifierTest {
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
