package com.example.ebookrecommendsystem.mapper;

import com.example.ebookrecommendsystem.entity.Review;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewMapper {
    @Insert("INSERT INTO review(user_id, book_id, content, status) VALUES(#{userId}, #{bookId}, #{content}, 1)")
    void insertReview(Review review);

    @Select("SELECT r.*, u.nickname FROM review r JOIN user u ON r.user_id = u.id WHERE r.book_id = #{bookId} AND r.status = 1 ORDER BY r.create_time DESC")
    List<Map<String, Object>> findReviewsByBookId(Integer bookId);

    @Select("SELECT r.*, u.nickname, b.title as bookTitle FROM review r JOIN user u ON r.user_id = u.id JOIN book b ON r.book_id = b.id ORDER BY r.create_time DESC")
    List<Map<String, Object>> findAllReviews();

    @org.apache.ibatis.annotations.Update("UPDATE review SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM review_like WHERE review_id = #{reviewId} AND user_id = #{userId}")
    int countLike(@Param("reviewId") Integer reviewId, @Param("userId") Integer userId);

    @Insert("INSERT INTO review_like(review_id, user_id) VALUES(#{reviewId}, #{userId})")
    void insertLike(@Param("reviewId") Integer reviewId, @Param("userId") Integer userId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM review_like WHERE review_id = #{reviewId} AND user_id = #{userId}")
    void deleteLike(@Param("reviewId") Integer reviewId, @Param("userId") Integer userId);

    @org.apache.ibatis.annotations.Update("UPDATE review SET like_count = like_count + 1 WHERE id = #{reviewId}")
    void incrLikeCount(@Param("reviewId") Integer reviewId);

    @org.apache.ibatis.annotations.Update("UPDATE review SET like_count = like_count - 1 WHERE id = #{reviewId} AND like_count > 0")
    void decrLikeCount(@Param("reviewId") Integer reviewId);

    @Select("SELECT review_id FROM review_like WHERE user_id = #{userId}")
    List<Integer> findLikedReviewIds(@Param("userId") Integer userId);
}