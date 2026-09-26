package com.example.ebookrecommendsystem.mapper;

import com.example.ebookrecommendsystem.entity.Bookshelf;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface BookshelfMapper {

    @Select("SELECT id, user_id AS userId, book_id AS bookId, read_status AS readStatus, create_time AS createTime " +
            "FROM bookshelf WHERE user_id = #{userId} AND book_id = #{bookId}")
    Bookshelf findByUserIdAndBookId(@Param("userId") Integer userId, @Param("bookId") Integer bookId);

    @Insert("INSERT INTO bookshelf(user_id, book_id, read_status) VALUES(#{userId}, #{bookId}, #{readStatus})")
    void insertBookshelf(Bookshelf bookshelf);

    @Update("UPDATE bookshelf SET read_status = #{readStatus} WHERE id = #{id}")
    void updateStatus(Bookshelf bookshelf);

    @Select("SELECT bs.read_status AS readStatus, b.* " +
            "FROM bookshelf bs JOIN book b ON bs.book_id = b.id " +
            "WHERE bs.user_id = #{userId} " +
            "ORDER BY bs.create_time DESC")
    List<Map<String, Object>> getBookshelfByUserId(@Param("userId") Integer userId);
    @org.apache.ibatis.annotations.Delete("DELETE FROM bookshelf WHERE user_id = #{userId} AND book_id = #{bookId}")
    void deleteBookshelf(@Param("userId") Integer userId, @Param("bookId") Integer bookId);
}