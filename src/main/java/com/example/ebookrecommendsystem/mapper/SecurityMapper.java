package com.example.ebookrecommendsystem.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SecurityMapper {
    @Insert("INSERT INTO security_account(user_id, token_version) VALUES(#{userId}, 0)")
    void createAccount(int userId);
    @Insert("INSERT INTO security_user_role(user_id, role_code) VALUES(#{userId}, 'USER')")
    void assignReader(int userId);
    @Select("SELECT token_version FROM security_account WHERE user_id=#{userId}")
    Integer version(int userId);
    @Select("SELECT token_version FROM security_account WHERE user_id=#{userId} FOR UPDATE")
    Integer lockVersion(int userId);
    @Update("UPDATE security_account SET token_version=token_version+1 WHERE user_id=#{userId}")
    void revokeAccess(int userId);
    @Select("SELECT role_code FROM security_user_role WHERE user_id=#{userId}")
    List<String> roles(int userId);
    @Select("SELECT DISTINCT rp.permission_code FROM security_user_role ur JOIN security_role_permission rp ON ur.role_code=rp.role_code WHERE ur.user_id=#{userId}")
    List<String> permissions(int userId);
    @Insert("INSERT INTO security_refresh(token_hash,user_id,token_version,expires_at) VALUES(#{hash},#{userId},#{version},#{expiresAt})")
    void saveRefresh(@Param("hash") String hash,@Param("userId") int userId,@Param("version") int version,@Param("expiresAt") long expiresAt);
    @Select("SELECT user_id FROM security_refresh WHERE token_hash=#{hash} AND expires_at>#{now}")
    Integer refreshUser(@Param("hash") String hash,@Param("now") long now);
    @Delete("DELETE FROM security_refresh WHERE token_hash=#{hash} AND token_version=#{version} AND expires_at>#{now}")
    int consumeRefresh(@Param("hash") String hash,@Param("version") int version,@Param("now") long now);
    @Delete("DELETE FROM security_refresh WHERE user_id=#{userId}")
    void revokeRefresh(int userId);
    @Delete("DELETE FROM security_refresh WHERE expires_at<=#{now}")
    void deleteExpired(long now);
}
