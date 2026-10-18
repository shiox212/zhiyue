package com.example.ebookrecommendsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RetrievalTest {
    @Test void unicodeChunksOverlapWithoutLosingText() {
        String original = "阅读📚".repeat(65);
        var chunks = new DocumentChunker(80, 10).split("book", "阅读", original);
        StringBuilder restored = new StringBuilder(chunks.get(0).text());
        for (int i = 1; i < chunks.size(); i++) {
            String text = chunks.get(i).text();
            restored.append(text.substring(text.offsetByCodePoints(0, 10)));
        }
        assertEquals(original, restored.toString());
        assertTrue(chunks.stream().allMatch(c -> c.text().codePointCount(0, c.text().length()) <= 80));
        assertEquals(chunks.size(), chunks.stream().map(KnowledgeChunk::id).distinct().count());
    }
    @Test void paragraphBoundariesAndEmptyInput() {
        var chunker = new DocumentChunker(80, 10);
        assertEquals(2, chunker.split("a", "测试", "第一段内容\n\n第二段内容").size());
        assertTrue(chunker.split("a", "测试", "  \n\n ").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new DocumentChunker(80, 80));
    }
    @Test void cosineRetrievalRanksRelevantDocumentAndRejectsUnknownTerms() {
        var index = new TfidfIndex(List.of(
                new KnowledgeChunk("auth_1", "auth", "登录", "JWT 登录令牌默认有效期900秒，支持刷新续期。"),
                new KnowledgeChunk("book_1", "book", "书架", "读者把图书加入书架，设置想读在读已读状态。")));
        assertEquals("auth_1", index.search("JWT 有效期是多少", 1, .13).get(0).chunk().id());
        assertTrue(index.search("哈雷彗星轨道周期", 3, .13).isEmpty());
        assertTrue(index.search("JWT", 3, .13).stream().allMatch(h -> h.score() >= 0 && h.score() <= 1.000001));
    }
    @Test void curatedCorpusRetrievesExpectedTopics() throws Exception {
        var knowledge = new KnowledgeBase(new ObjectMapper());
        assertEquals(6, knowledge.documentCount());
        for (var entry : List.of(new String[]{"IDEA 启动方法", "running"},
                new String[]{"JWT 过期刷新", "authentication"}, new String[]{"RBAC 角色权限", "authorization"})) {
            assertTrue(knowledge.retrieve(entry[0]).stream().anyMatch(h -> h.chunk().documentId().equals(entry[1])), entry[0]);
        }
        assertTrue(knowledge.retrieve("哈雷彗星轨道周期").isEmpty());
    }
}
