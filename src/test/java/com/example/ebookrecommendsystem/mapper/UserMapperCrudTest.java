package com.example.ebookrecommendsystem.mapper;

import com.example.ebookrecommendsystem.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserMapper 用户 CRUD")
class UserMapperCrudTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS user (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                password VARCHAR(100) NOT NULL,
                nickname VARCHAR(50),
                role TINYINT DEFAULT 0,
                status TINYINT DEFAULT 1,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
        jdbc.execute("DELETE FROM user");
    }

    @Test
    @DisplayName("新增、查询、更新、列表与删除形成完整闭环")
    void shouldCompleteCrudLifecycle() {
        User user = new User();
        user.setUsername("crud_user");
        user.setPassword("$2a$10$abcdefghijklmnopqrstuv0123456789012345678901234567890");
        user.setNickname("初始昵称");

        assertEquals(1, userMapper.insertUser(user));
        assertNotNull(user.getId());

        User created = userMapper.findById(user.getId());
        assertEquals("crud_user", created.getUsername());
        assertEquals("初始昵称", created.getNickname());
        assertEquals(0, created.getRole());
        assertEquals(1, created.getStatus());

        assertEquals(1, userMapper.updateProfile(user.getId(), "更新昵称"));
        assertEquals(1, userMapper.updateStatus(user.getId(), 0));
        assertEquals(1, userMapper.updatePassword(user.getId(), "$2a$10$updatedHash"));

        User updated = userMapper.findByUsername("crud_user");
        assertEquals("更新昵称", updated.getNickname());
        assertEquals(0, updated.getStatus());
        assertEquals("$2a$10$updatedHash", updated.getPassword());
        assertEquals(1, userMapper.findAll().size());

        assertEquals(1, userMapper.deleteById(user.getId()));
        assertNull(userMapper.findById(user.getId()));
    }
}
