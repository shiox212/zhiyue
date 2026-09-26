package com.example.ebookrecommendsystem.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {

    @Select("SELECT category AS name, COUNT(*) AS value FROM book GROUP BY category")
    List<Map<String, Object>> getCategoryStats();


    @Select("SELECT b.title AS name, ROUND(AVG(r.score), 1) AS value " +
            "FROM rating r JOIN book b ON r.book_id = b.id " +
            "GROUP BY b.id, b.title " +
            "ORDER BY value DESC LIMIT 5")
    List<Map<String, Object>> getTopRatedBooks();

    @Select("SELECT COUNT(*) FROM book")
    int countBooks();

    @Select("SELECT COUNT(*) FROM user")
    int countUsers();

    @Select("SELECT COUNT(*) FROM rating")
    int countRatings();

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS count " +
            "FROM user GROUP BY month ORDER BY month")
    List<Map<String, Object>> getUserGrowth();

    @Select("SELECT GREATEST(FLOOR(score), 1) AS star, COUNT(*) AS count " +
            "FROM rating WHERE book_id = #{bookId} GROUP BY star ORDER BY star")
    List<Map<String, Object>> getRatingDistribution(Integer bookId);
}