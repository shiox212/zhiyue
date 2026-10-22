package com.example.ebookrecommendsystem.mapper;

import com.example.ebookrecommendsystem.entity.Book;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BookMapper {

    String ASSISTANT_TITLE_MATCH = "(LENGTH(TRIM(title)) > 0 AND LOCATE(LOWER(TRIM(title)), LOWER(#{question})) > 0)";
    String ASSISTANT_AUTHOR_MATCH = "(LENGTH(TRIM(author)) > 0 AND LOCATE(LOWER(TRIM(author)), LOWER(#{question})) > 0)";

    // Assistant-only public metadata projection; bound keywords and a fixed candidate ceiling.
    @Select("<script>SELECT id, title, author, category, description FROM book WHERE (1=0 " +
            "<foreach collection='keywords' item='keyword'>" +
            "OR (LOCATE(#{keyword}, title) > 0 OR LOCATE(#{keyword}, author) > 0 " +
            "OR LOCATE(#{keyword}, category) > 0 OR LOCATE(#{keyword}, description) > 0)" +
            "</foreach>) OR " + ASSISTANT_TITLE_MATCH + " OR " + ASSISTANT_AUTHOR_MATCH + " ORDER BY CASE " +
            "WHEN " + ASSISTANT_TITLE_MATCH + " AND " + ASSISTANT_AUTHOR_MATCH + " THEN 0 " +
            "WHEN " + ASSISTANT_TITLE_MATCH + " THEN 1 " +
            "WHEN " + ASSISTANT_AUTHOR_MATCH + " THEN 2 " +
            "ELSE 3 END, CASE " +
            "WHEN " + ASSISTANT_AUTHOR_MATCH + " THEN LENGTH(TRIM(author)) " +
            "WHEN " + ASSISTANT_TITLE_MATCH + " THEN LENGTH(TRIM(title)) " +
            "ELSE 0 END DESC, id LIMIT 20</script>")
    List<Book> findAssistantCandidates(@Param("keywords") List<String> keywords, @Param("question") String question);

    @Select("SELECT * FROM book ORDER BY create_time DESC")
    List<Book> findAll();

    @Insert("INSERT INTO book(title, author, category, cover_url, description) " +
            "VALUES(#{title}, #{author}, #{category}, #{coverUrl}, #{description})")
    void insertBook(Book book);

    @Select("<script>" +
            "SELECT * FROM book WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Book> findByIds(@Param("ids") java.util.List<Integer> ids);

    @Select("SELECT b.*, (SELECT AVG(r.score) FROM rating r WHERE r.book_id=b.id) AS avg_score, " +
            "(SELECT COUNT(*) FROM rating r WHERE r.book_id=b.id) AS rating_count, " +
            "(SELECT COUNT(*) FROM review rv WHERE rv.book_id=b.id AND rv.status=1) AS review_count " +
            "FROM book b WHERE b.id = #{id}")
    Book findById(Integer id);

    @Select("SELECT * FROM book WHERE title LIKE CONCAT('%', #{keyword}, '%') OR author LIKE CONCAT('%', #{keyword}, '%') ORDER BY create_time DESC")
    List<Book> searchByKeyword(@Param("keyword") String keyword);

    @Select("SELECT * FROM book WHERE " +
            "(title LIKE CONCAT('%', #{keyword}, '%') OR author LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND category = #{category} " +
            "ORDER BY create_time DESC")
    List<Book> searchByKeywordAndCategory(@Param("keyword") String keyword, @Param("category") String category);

    @org.apache.ibatis.annotations.Update("UPDATE book SET title=#{title}, author=#{author}, category=#{category}, cover_url=#{coverUrl}, description=#{description} WHERE id=#{id}")
    void updateBook(Book book);

    @org.apache.ibatis.annotations.Delete("DELETE FROM book WHERE id = #{id}")
    void deleteById(Integer id);

    @Select("SELECT DISTINCT category FROM book WHERE category IS NOT NULL ORDER BY category")
    List<String> findAllCategories();

    @Select("SELECT COUNT(*) FROM book WHERE category = #{category}")
    int countByCategory(@Param("category") String category);

    @Select("SELECT * FROM book WHERE category = #{category} ORDER BY create_time DESC")
    List<Book> findByCategory(@Param("category") String category);

    @Select("SELECT b.* FROM book b " +
            "LEFT JOIN rating r ON b.id = r.book_id " +
            "GROUP BY b.id " +
            "ORDER BY COUNT(r.id) DESC, COALESCE(AVG(r.score), 0) DESC " +
            "LIMIT #{limit}")
    List<Book> findHotBooks(@Param("limit") int limit);

    @Select("SELECT * FROM book WHERE id != #{bookId} " +
            "AND (category = (SELECT category FROM book WHERE id = #{bookId}) " +
            "OR author = (SELECT author FROM book WHERE id = #{bookId})) " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<Book> findSimilarBooks(@Param("bookId") Integer bookId, @Param("limit") int limit);

    @Select("SELECT t.*, ROW_NUMBER() OVER (ORDER BY t.avg_score DESC, t.rating_count DESC) as rank_num " +
            "FROM (" +
            "  SELECT b.*, COALESCE(AVG(r.score), 0) as avg_score, COUNT(DISTINCT r.id) as rating_count, " +
            "  COUNT(DISTINCT rv.id) as review_count " +
            "  FROM book b LEFT JOIN rating r ON b.id = r.book_id " +
            "  LEFT JOIN review rv ON b.id = rv.book_id AND rv.status = 1 " +
            "  GROUP BY b.id" +
            ") t " +
            "ORDER BY t.avg_score DESC, t.rating_count DESC")
    List<Book> findAllByRatingRanking();

    @Select("SELECT t.*, ROW_NUMBER() OVER (ORDER BY t.rating_count DESC, t.avg_score DESC) as rank_num " +
            "FROM (" +
            "  SELECT b.*, COALESCE(AVG(r.score), 0) as avg_score, COUNT(DISTINCT r.id) as rating_count, " +
            "  COUNT(DISTINCT rv.id) as review_count " +
            "  FROM book b LEFT JOIN rating r ON b.id = r.book_id " +
            "  LEFT JOIN review rv ON b.id = rv.book_id AND rv.status = 1 " +
            "  GROUP BY b.id" +
            ") t " +
            "ORDER BY t.rating_count DESC, t.avg_score DESC")
    List<Book> findAllByPopularityRanking();
}
