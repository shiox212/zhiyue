package com.example.ebookrecommendsystem.mapper;

import com.example.ebookrecommendsystem.entity.Book;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BookMapper {

    // Assistant-only public metadata projection; bound keywords and a fixed candidate ceiling.
    @Select("<script>SELECT id, title, author, category, description FROM book WHERE " +
            "<foreach collection='keywords' item='keyword' open='(' separator=' OR ' close=')'>" +
            "(LOCATE(#{keyword}, title) > 0 OR LOCATE(#{keyword}, author) > 0 " +
            "OR LOCATE(#{keyword}, category) > 0 OR LOCATE(#{keyword}, description) > 0)" +
            "</foreach> ORDER BY CASE " +
            "WHEN title IS NOT NULL AND LENGTH(TRIM(title)) > 0 AND LOCATE(LOWER(title), LOWER(#{question})) > 0 THEN 0 " +
            "WHEN author IS NOT NULL AND LENGTH(TRIM(author)) > 0 AND LOCATE(LOWER(author), LOWER(#{question})) > 0 THEN 1 " +
            "ELSE 2 END, CASE " +
            "WHEN title IS NOT NULL AND LENGTH(TRIM(title)) > 0 AND LOCATE(LOWER(title), LOWER(#{question})) > 0 THEN LENGTH(TRIM(title)) " +
            "WHEN author IS NOT NULL AND LENGTH(TRIM(author)) > 0 AND LOCATE(LOWER(author), LOWER(#{question})) > 0 THEN LENGTH(TRIM(author)) " +
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

    @Select("SELECT * FROM book WHERE id = #{id}")
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
