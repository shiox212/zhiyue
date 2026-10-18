package com.example.ebookrecommendsystem.ai;

import com.example.ebookrecommendsystem.entity.Book;
import com.example.ebookrecommendsystem.mapper.BookMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BookKnowledgeTest {
    @Test void publicMetadataIsBoundedAndCitationsHaveStableBookIds() {
        var mapper = mock(BookMapper.class);
        var book = new Book();
        book.setId(7); book.setTitle("三体"); book.setAuthor("刘慈欣"); book.setCategory("科幻小说");
        book.setDescription("三体是一部科幻小说。".repeat(250));
        book.setCoverUrl("private-cover-value"); book.setAvgScore(4.9);
        when(mapper.findAssistantCandidates(anyList(), anyString())).thenAnswer(invocation -> {
            List<String> keywords = invocation.getArgument(0);
            assertTrue(keywords.size() <= 32); assertTrue(keywords.contains("三体"));
            return List.of(book);
        });
        var hits = new BookKnowledge(mapper).retrieve("三体科幻小说作者简介");
        assertFalse(hits.isEmpty()); assertTrue(hits.size() <= 3);
        for (var hit : hits) {
            assertEquals(7, hit.chunk().bookId()); assertTrue(hit.chunk().id().matches("book_7_\\d+"));
            assertTrue(hit.chunk().text().length() <= 480);
            assertFalse(hit.chunk().text().contains("private-cover-value")); assertFalse(hit.chunk().text().contains("4.9"));
        }
        verify(mapper, never()).findAll();
    }
    @Test void emptySearchDoesNotFetchEntireLibrary() {
        var mapper = mock(BookMapper.class);
        assertTrue(new BookKnowledge(mapper).retrieve("？").isEmpty()); verifyNoInteractions(mapper);
    }
    @Test void completeTitleOutranksDescriptionsWithManyIncidentalQueryMatches() {
        var mapper = mock(BookMapper.class);
        var target = book(99, "银河帝国", "阿西莫夫", "远未来的故事。");
        var distractors = java.util.stream.IntStream.rangeClosed(1, 19)
                .mapToObj(i -> book(i, "科幻集" + i, "其他作者", "银河 帝国 简介 作者 银河帝国作者简介".repeat(12))).toList();
        var candidates = new java.util.ArrayList<>(distractors); candidates.add(target);
        when(mapper.findAssistantCandidates(anyList(), eq("银河帝国作者简介"))).thenReturn(candidates);
        var hits = new BookKnowledge(mapper).retrieve("银河帝国作者简介");
        assertFalse(hits.isEmpty()); assertEquals(99, hits.get(0).chunk().bookId());
        assertTrue(hits.get(0).score() > 2);
    }
    @Test void completeAuthorOutranksPartialAuthorMatchesAndEmptyValuesNeverReceivePriority() {
        var mapper = mock(BookMapper.class);
        when(mapper.findAssistantCandidates(anyList(), anyString())).thenReturn(List.of(
                book(1, "", "", "刘慈 欣的作品作者简介"),
                book(2, "作品集", "刘慈明", "刘慈 欣的作品作者简介".repeat(5)),
                book(88, "球状闪电", "刘慈欣", "一种自然现象。")));
        var hits = new BookKnowledge(mapper).retrieve("刘慈欣作品简介");
        assertEquals(88, hits.get(0).chunk().bookId()); assertTrue(hits.get(0).score() > 1);
    }
    private static Book book(int id, String title, String author, String description) {
        var book = new Book(); book.setId(id); book.setTitle(title); book.setAuthor(author);
        book.setCategory("小说"); book.setDescription(description); return book;
    }

    @Test void exactShortTitleSurvivesLowSimilarityOfLongMetadata() {
        var mapper = mock(BookMapper.class);
        var target = book(71, "诗经", "佚名", "");
        target.setDescription(java.util.stream.IntStream.range(0, 400)
                .mapToObj(i -> "词" + i).collect(java.util.stream.Collectors.joining(" ")));
        when(mapper.findAssistantCandidates(anyList(), anyString())).thenReturn(List.of(target));
        assertEquals(71, new BookKnowledge(mapper).retrieve("《诗经》的作者和简介是什么？").get(0).chunk().bookId());
    }

    @Test void exactPunctuatedAuthorSurvivesLowSimilarity() {
        var mapper = mock(BookMapper.class);
        var target = book(98, "哈姆雷特", "威廉·莎士比亚", "独白、装疯与戏中戏呈现复仇和犹疑。");
        when(mapper.findAssistantCandidates(anyList(), anyString())).thenReturn(List.of(target));
        assertEquals(98, new BookKnowledge(mapper).retrieve("威廉·莎士比亚的作品有哪些？").get(0).chunk().bookId());
    }

    @Test void sameTitleKeepsBothWorksAndExplicitAuthorRanksFirst() {
        var mapper = mock(BookMapper.class);
        when(mapper.findAssistantCandidates(anyList(), anyString())).thenReturn(List.of(
                book(93, "变形记", "奥维德", "古罗马神话叙事诗。"),
                book(132, "变形记", "弗兰茨·卡夫卡", "格里高尔的家庭关系与劳动身份。")));
        var knowledge = new BookKnowledge(mapper);
        assertEquals(java.util.Set.of(93,132), knowledge.retrieve("《变形记》的作者和简介是什么？").stream()
                .map(h -> h.chunk().bookId()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(132, knowledge.retrieve("弗兰茨·卡夫卡的《变形记》简介").get(0).chunk().bookId());
    }

    @Test void lowSimilarityWithoutExactTitleOrAuthorStillHasNoEvidence() {
        var mapper = mock(BookMapper.class);
        var target = book(8, "诗歌总集", "古代编者", "山水与生活经验。");
        when(mapper.findAssistantCandidates(anyList(), anyString())).thenReturn(List.of(target));
        assertTrue(new BookKnowledge(mapper).retrieve("qzvxjkp729").isEmpty());
    }

    @Test void completePunctuatedTitleDoesNotRequireBigrams() {
        var mapper = mock(BookMapper.class);
        when(mapper.findAssistantCandidates(anyList(), anyString())).thenReturn(List.of(
                book(110, "简·爱", "夏洛蒂·勃朗特", "成长与独立。")));
        var hits = new BookKnowledge(mapper).retrieve("简·爱");
        assertFalse(hits.isEmpty()); assertEquals(110, hits.get(0).chunk().bookId());
    }
}
