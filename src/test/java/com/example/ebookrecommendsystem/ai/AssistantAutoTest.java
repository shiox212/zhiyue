package com.example.ebookrecommendsystem.ai;

import com.example.ebookrecommendsystem.security.PromptInputPolicy;
import com.example.ebookrecommendsystem.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AssistantAutoTest {
    final ObjectMapper json = new ObjectMapper();
    final KnowledgeBase knowledge = mock(KnowledgeBase.class);
    final BookKnowledge books = mock(BookKnowledge.class);
    final LanguageModel model = mock(LanguageModel.class);
    final AiRequestGuard guard = mock(AiRequestGuard.class);
    LearningAssistant service() { return new LearningAssistant(knowledge, books, model, new PromptInputPolicy(), new AnswerVerifier(json), guard, json); }
    TfidfIndex.Hit book(int id) { return new TfidfIndex.Hit(new KnowledgeChunk("book_"+id+"_1", "book_"+id, "测试书"+id, "书名：测试书\n作者：测试作者\n简介：这是公开的书目简介。", id), .4); }
    void output(Map<String,Object> value) throws Exception {
        when(model.ready()).thenReturn(true);
        when(model.generate(anyString(), anyString())).thenReturn(new LanguageModel.Generation(json.writeValueAsString(value), "fixture", "fixture", 1, 1));
    }
    @Test void noHitGeneralOnceAndNeverTrustsModelBookIds() throws Exception {
        output(Map.of("status","general","answer","可以从短篇读起。","citations",List.of(),"relatedBooks",List.of(Map.of("bookId",999,"title","虚构"))));
        var result=service().ask(1,"文学阅读建议");
        assertEquals("GENERAL_KNOWLEDGE",result.status()); assertTrue(result.answer().startsWith("本次未检索到"));
        assertTrue(result.relatedBooks().isEmpty()); assertTrue(result.citations().isEmpty());
        verify(model,times(1)).generate(eq(LearningAssistant.GENERAL_SYSTEM),anyString());
    }
    @Test void relevantBookSurvivesProjectCompetitionAndCardsDeduplicate() throws Exception {
        when(books.retrieve(anyString())).thenReturn(List.of(book(7),book(7),book(8)));
        when(knowledge.retrieve(anyString())).thenReturn(List.of(
            new TfidfIndex.Hit(new KnowledgeChunk("project_1","project","项目","项目资料仅说明运行方法。"),.9),
            new TfidfIndex.Hit(new KnowledgeChunk("project_2","project","项目","项目资料仅说明配置方法。"),.8),
            new TfidfIndex.Hit(new KnowledgeChunk("project_3","project","项目","项目资料仅说明登录方法。"),.7)));
        output(Map.of("status","answered","answer","作者为测试作者。","citations",List.of(Map.of("sourceId","book_7_1","quote","作者：测试作者","bookId",999)),"relatedBooks",List.of(Map.of("bookId",999))));
        var result=service().ask(1,"测试书作者");
        assertEquals("ANSWERED",result.status());assertEquals(7,result.citations().get(0).bookId());
        assertEquals(List.of(7,8),result.relatedBooks().stream().map(LearningAssistant.RelatedBook::bookId).toList());
        verify(model,times(1)).generate(eq(LearningAssistant.AUTO_SYSTEM),anyString());
    }
    @Test void partialMetadataHasOneGenerationAndSeparateSupplement() throws Exception {
        when(books.retrieve(anyString())).thenReturn(List.of(book(7)));
        output(Map.of("status","insufficient","answer","资料不足","citations",List.of(),"generalSupplement","通用阅读建议：记录人物的选择与变化。"));
        var result=service().ask(1,"测试书人物怎样理解");
        assertEquals("INSUFFICIENT",result.status());assertTrue(result.generalSupplement().contains("通用阅读"));
        assertEquals(7,result.relatedBooks().get(0).bookId());assertTrue(result.citations().isEmpty());
        verify(model,times(1)).generate(anyString(),anyString());
    }
    @Test void projectOnlyHasNoBookCards() throws Exception {
        when(knowledge.retrieve(anyString())).thenReturn(List.of(new TfidfIndex.Hit(new KnowledgeChunk("project_1","project","项目","项目支持公开图书检索。"),.8)));
        output(Map.of("status","answered","answer","支持检索。","citations",List.of(Map.of("sourceId","project_1","quote","项目支持公开图书检索。"))));
        var result=service().ask(1,"项目支持检索吗");assertEquals("ANSWERED",result.status());assertTrue(result.relatedBooks().isEmpty());
        verify(model,times(1)).generate(anyString(),anyString());
    }
    @Test void databaseFailureNeverFallsBack() {
        when(books.retrieve(anyString())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("fixture"));
        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,()->service().ask(1,"测试书"));verifyNoInteractions(model);
    }
    @Test void failedQuotesCannotBeRelabelledGeneralOrRetried() throws Exception {
        when(books.retrieve(anyString())).thenReturn(List.of(book(7)));
        for (var refs:List.of(List.of(Map.of("sourceId","book_7_1","quote","作者：错误作者")),Collections.nCopies(2,Map.of("sourceId","book_7_1","quote","作者：测试作者")))) {
            reset(model);output(Map.of("status","answered","answer","不要展示这句","citations",refs,"generalSupplement","也不能展示补充"));
            var result=service().ask(1,"测试书作者");assertEquals("UNVERIFIED",result.status());assertNull(result.generalSupplement());assertFalse(result.answer().contains("不要展示"));assertEquals(1,result.relatedBooks().size());
            verify(model,times(1)).generate(anyString(),anyString());
        }
    }
    @Test void illegalSupplementNeverLeaksSourceLinks() throws Exception {
        when(books.retrieve(anyString())).thenReturn(List.of(book(7)));
        for(Object part:List.of("请看 /books/999",Map.of("answer","错误类型"),"引用[1]说明", "book_999_1")) {
            reset(model);output(Map.of("status","insufficient","answer","资料不足","citations",List.of(),"generalSupplement",part));
            var result=service().ask(1,"测试书分析");assertEquals("UNVERIFIED",result.status());assertNull(result.generalSupplement());verify(model,times(1)).generate(anyString(),anyString());
        }
    }
    @Test void allModesRejectBlankAndPunctuationBeforeGenerating() {
        for(var mode:LearningAssistant.Mode.values()) for(String q:List.of(" ","？！","……")) assertThrows(BusinessException.class,()->service().ask(1,q,mode));
        verifyNoInteractions(model,books,knowledge);
    }
    @Test void timeoutAndMalformedOutputAreNeverRetried() {
        when(model.ready()).thenReturn(true);when(model.generate(anyString(),anyString())).thenThrow(new AiServiceException(503,"模型请求超时"));
        assertThrows(AiServiceException.class,()->service().ask(1,"阅读建议"));verify(model,times(1)).generate(anyString(),anyString());
        reset(model);when(model.ready()).thenReturn(true);when(model.generate(anyString(),anyString())).thenReturn(new LanguageModel.Generation("invalid","fixture","fixture",0,0));
        assertEquals("UNVERIFIED",service().ask(1,"阅读建议").status());verify(model,times(1)).generate(anyString(),anyString());
    }
    @Test void rateLimitStopsBeforeGeneration() {
        when(model.ready()).thenReturn(true);when(guard.acquire(1)).thenThrow(new AiServiceException(429,"稍后再试"));
        assertEquals(429,assertThrows(AiServiceException.class,()->service().ask(1,"阅读建议")).status());verify(model,never()).generate(anyString(),anyString());
    }

    @Test void explicitLibraryRetainsItsOriginalPromptAndIgnoresSupplement() throws Exception {
        when(books.retrieve(anyString())).thenReturn(List.of(book(7)));
        output(Map.of("status","answered","answer","作者为测试作者。","citations",List.of(Map.of("sourceId","book_7_1","quote","作者：测试作者")),"generalSupplement","不应显示"));
        var result=service().ask(1,"测试书作者",LearningAssistant.Mode.LIBRARY);
        assertEquals("ANSWERED",result.status());assertNull(result.generalSupplement());
        verify(model,times(1)).generate(eq(LearningAssistant.SYSTEM),anyString());
    }
    @Test void unrelatedBookCandidateIsNotForcedIntoAutoCards() throws Exception {
        var mapper=mock(com.example.ebookrecommendsystem.mapper.BookMapper.class);
        var b=new com.example.ebookrecommendsystem.entity.Book();b.setId(9);b.setTitle("海洋学");b.setAuthor("测试作者");b.setDescription("讨论潮汐和海水循环。");
        when(mapper.findAssistantCandidates(anyList(),anyString())).thenReturn(List.of(b));
        var retrieval=new BookKnowledge(mapper);
        assertTrue(retrieval.retrieve("哈雷彗星轨道周期").isEmpty());
        output(Map.of("status","general","answer","通用知识解释。","citations",List.of()));
        var result=new LearningAssistant(knowledge,retrieval,model,new PromptInputPolicy(),new AnswerVerifier(json),guard,json).ask(1,"哈雷彗星轨道周期");
        assertEquals("GENERAL_KNOWLEDGE",result.status());assertTrue(result.relatedBooks().isEmpty());verify(model,times(1)).generate(anyString(),anyString());
    }
    @Test void supplementCannotReferToProjectSourceIds() throws Exception {
        when(knowledge.retrieve(anyString())).thenReturn(List.of(new TfidfIndex.Hit(new KnowledgeChunk("auth_1","auth","登录","项目使用安全的登录机制。"),.8)));
        output(Map.of("status","insufficient","answer","资料不足","citations",List.of(),"generalSupplement","根据 auth_1 可知其他情况。"));
        assertEquals("UNVERIFIED",service().ask(1,"登录机制分析").status());verify(model,times(1)).generate(anyString(),anyString());
    }
}
