package com.example.ebookrecommendsystem.mapper;

import java.util.List;
import com.example.ebookrecommendsystem.entity.Rating;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RatingMapper {

    @Select("SELECT * FROM rating WHERE user_id = #{userId} AND book_id = #{bookId}")
    Rating findByUserIdAndBookId(Integer userId, Integer bookId);

    @Insert("INSERT INTO rating(user_id, book_id, score) VALUES(#{userId}, #{bookId}, #{score})")
    void insertRating(Rating rating);

    @Update("UPDATE rating SET score = #{score} WHERE id = #{id}")
    void updateScore(Rating rating);

    @Select("SELECT * FROM rating")
    List<Rating> findAllRatings();
}