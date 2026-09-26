package com.example.ebookrecommendsystem.mapper;

import com.example.ebookrecommendsystem.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Integer id);

    @Insert("INSERT INTO user(username, password, nickname, role, status) " +
            "VALUES(#{username}, #{password}, #{nickname}, 0, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    @Select("SELECT * FROM user ORDER BY create_time DESC")
    List<User> findAll();

    @Update("UPDATE user SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Update("UPDATE user SET nickname = #{nickname} WHERE id = #{id}")
    int updateProfile(@Param("id") Integer id, @Param("nickname") String nickname);

    @Update("UPDATE user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    @Select("SELECT password FROM user WHERE id = #{id}")
    String getPasswordById(Integer id);

    @Select("SELECT COUNT(*) FROM bookshelf WHERE user_id = #{userId}")
    int countBookshelfByUserId(Integer userId);

    @Select("SELECT COUNT(*) FROM rating WHERE user_id = #{userId}")
    int countRatingsByUserId(Integer userId);

    @Select("SELECT COUNT(*) FROM review WHERE user_id = #{userId}")
    int countReviewsByUserId(Integer userId);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(Integer id);
}
