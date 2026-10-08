package com.example.ebookrecommendsystem.mapper;

import com.example.ebookrecommendsystem.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MybatisTest
@ActiveProfiles("test")
@DisplayName("BookMapper 排行查询")
class BookMapperTest {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);

        // 创建 book 表（兼容 H2 MySQL 模式）
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS book (
                id INT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(200) NOT NULL,
                author VARCHAR(100),
                category VARCHAR(50),
                cover_url TEXT,
                description TEXT,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        // 创建 rating 表
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS rating (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                book_id INT NOT NULL,
                score DECIMAL(2,1) NOT NULL,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        // 创建 review 表
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS review (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                book_id INT NOT NULL,
                content TEXT NOT NULL,
                status TINYINT DEFAULT 1,
                like_count INT DEFAULT 0,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        // 插入 3 本测试图书
        jdbc.execute("INSERT INTO book(id, title, author, category) VALUES (1, '三体', '刘慈欣', '科幻小说')");
        jdbc.execute("INSERT INTO book(id, title, author, category) VALUES (2, '活着', '余华', '文学经典')");
        jdbc.execute("INSERT INTO book(id, title, author, category) VALUES (3, '银河帝国', '阿西莫夫', '科幻小说')");

        // 图书1：3 人评分 (5.0, 4.0, 3.0)，均分 4.0，2 条已通过书评
        jdbc.execute("INSERT INTO rating(user_id, book_id, score) VALUES (1, 1, 5.0)");
        jdbc.execute("INSERT INTO rating(user_id, book_id, score) VALUES (2, 1, 4.0)");
        jdbc.execute("INSERT INTO rating(user_id, book_id, score) VALUES (3, 1, 3.0)");
        jdbc.execute("INSERT INTO review(id, user_id, book_id, content, status) VALUES (1, 1, 1, '精彩', 1)");
        jdbc.execute("INSERT INTO review(id, user_id, book_id, content, status) VALUES (2, 2, 1, '好看', 1)");

        // 图书2：2 人评分 (5.0, 5.0)，均分 5.0，1 条已通过 + 1 条待审核
        jdbc.execute("INSERT INTO rating(user_id, book_id, score) VALUES (1, 2, 5.0)");
        jdbc.execute("INSERT INTO rating(user_id, book_id, score) VALUES (2, 2, 5.0)");
        jdbc.execute("INSERT INTO review(id, user_id, book_id, content, status) VALUES (3, 1, 2, '经典之作', 1)");
        jdbc.execute("INSERT INTO review(id, user_id, book_id, content, status) VALUES (4, 2, 2, '审核中', 0)");

        // 图书3：无评分，无书评
    }

    @Test
    @DisplayName("评分排行：按均分降序，含 rankNum 和 reviewCount")
    void shouldReturnBooksOrderedByAvgScoreWithRankAndReviewCount() {
        List<Book> books = bookMapper.findAllByRatingRanking();

        assertEquals(3, books.size());

        // 图书2（均分5.0）排第一
        assertEquals(2, books.get(0).getId());
        assertEquals(1, books.get(0).getRankNum());
        assertEquals(1, books.get(0).getReviewCount()); // 仅1条 status=1 的书评

        // 图书1（均分4.0）排第二
        assertEquals(1, books.get(1).getId());
        assertEquals(2, books.get(1).getRankNum());
        assertEquals(2, books.get(1).getReviewCount()); // 2条已通过书评

        // 图书3（均分0.0）排第三
        assertEquals(3, books.get(2).getId());
        assertEquals(3, books.get(2).getRankNum());
        assertEquals(0, books.get(2).getReviewCount());
    }

    @Test
    @DisplayName("COUNT(DISTINCT) 不受笛卡尔积影响")
    void shouldNotInflateCountsDueToCartesianProduct() {
        // 图书1 有 3 条评分和 2 条已通过书评
        // 如果不用 COUNT(DISTINCT)，LEFT JOIN 会产生 3×2=6 行，ratingCount 会变成 6
        List<Book> books = bookMapper.findAllByRatingRanking();

        Book book1 = books.stream().filter(b -> b.getId() == 1).findFirst().orElseThrow();

        assertEquals(3, book1.getRatingCount(),
                "ratingCount 应为 3，而非 6（笛卡尔积会导致 3×2=6）");
        assertEquals(2, book1.getReviewCount(),
                "reviewCount 应为 2（仅已通过书评）");
        assertNotNull(book1.getAvgScore());
        assertEquals(4.0, book1.getAvgScore(), 0.01); // (5+4+3)/3
    }

    @Test void assistantProjectsOnlyMatchingPublicMetadataAndTreatsWildcardsLiterally() {
        var books = bookMapper.findAssistantCandidates(List.of("刘慈欣"), "刘慈欣");
        assertEquals(1, books.size()); assertEquals("三体", books.get(0).getTitle());
        assertNull(books.get(0).getCreateTime()); assertNull(books.get(0).getCoverUrl());
        assertTrue(bookMapper.findAssistantCandidates(List.of("%", "_", "' OR 1=1 --"), "' OR 1=1 --").isEmpty());
    }
    @Test void assistantKeepsExactHighIdTitleAndAuthorAheadOfMoreThanTwentyGenericCandidates() {
        for (int id = 10; id < 35; id++) {
            jdbc.update("INSERT INTO book(id, title, author, description) VALUES (?, ?, ?, ?)",
                    id, "其他图书" + id, "其他作者" + id, "图书作者简介");
        }
        jdbc.update("INSERT INTO book(id, title, author, description) VALUES (99, '银河帝国选集', '阿西莫夫', '图书作者简介')");
        var byTitle = bookMapper.findAssistantCandidates(List.of("作者", "简介", "银河"), "银河帝国选集的作者和简介");
        assertEquals(20, byTitle.size()); assertEquals(99, byTitle.get(0).getId());
        var byAuthor = bookMapper.findAssistantCandidates(List.of("作者", "简介", "阿西"), "阿西莫夫的作品简介");
        assertEquals(20, byAuthor.size()); assertTrue(byAuthor.subList(0, 2).stream().anyMatch(book -> book.getId() == 99));
    }
    @Test
    @DisplayName("热门排行：按评分人数降序")
    void shouldReturnBooksOrderedByRatingCount() {
        List<Book> books = bookMapper.findAllByPopularityRanking();

        assertEquals(3, books.size());
        // 图书1（3人评分）排第一
        assertEquals(1, books.get(0).getId());
        assertEquals(1, books.get(0).getRankNum());
        // 图书2（2人评分）排第二
        assertEquals(2, books.get(1).getId());
        assertEquals(2, books.get(1).getRankNum());
        // 图书3（0人评分）排第三
        assertEquals(3, books.get(2).getId());
    }
}
