package com.example.ebookrecommendsystem.service;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.User;
import com.example.ebookrecommendsystem.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 用户注册、登录与维护")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User dbUser;

    @BeforeEach
    void setUp() {
        dbUser = new User();
        dbUser.setId(2);
        dbUser.setUsername("zhangsan");
        dbUser.setPassword("$2a$10$hashedBcryptPasswordHere");
        dbUser.setNickname("张三");
        dbUser.setRole(0);
        dbUser.setStatus(1);
    }

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册成功：新用户名 + 密码加密存储")
    void shouldRegisterSuccessfully() {
        when(userMapper.findByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("$2a$10$encrypted");
        when(userMapper.insertUser(any(User.class))).thenReturn(1);

        User input = new User();
        input.setUsername("newuser");
        input.setPassword("123456");

        Result<Object> result = userService.register(input);

        assertEquals(200, result.getCode());
        assertEquals("注册成功！", result.getMsg());
        assertEquals("热心读者", input.getNickname()); // 默认昵称
        assertEquals("$2a$10$encrypted", input.getPassword()); // 密码已加密
        verify(userMapper).insertUser(input);
    }

    @Test
    @DisplayName("注册失败：用户名已被占用")
    void shouldFailWhenUsernameExists() {
        User existing = new User();
        existing.setUsername("zhangsan");
        when(userMapper.findByUsername("zhangsan")).thenReturn(existing);

        User input = new User();
        input.setUsername("zhangsan");
        input.setPassword("123456");

        Result<Object> result = userService.register(input);

        assertEquals(500, result.getCode());
        assertEquals("注册失败：该账号已被占用！", result.getMsg());
        verify(userMapper, never()).insertUser(any());
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录成功：BCrypt 密码匹配")
    void shouldLoginSuccessfully() {
        when(userMapper.findByUsername("zhangsan")).thenReturn(dbUser);
        when(passwordEncoder.matches("123456", dbUser.getPassword())).thenReturn(true);

        User input = new User();
        input.setUsername("zhangsan");
        input.setPassword("123456");

        Result<User> result = userService.login(input);

        assertEquals(200, result.getCode());
        assertTrue(result.getMsg().startsWith("登录成功！"));
        assertEquals("张三", result.getData().getNickname());
        assertNull(result.getData().getPassword()); // 密码已清除
    }

    @Test
    @DisplayName("登录成功：明文密码回退兼容")
    void shouldLoginWithPlaintextFallback() {
        dbUser.setPassword("123456"); // 明文存储的密码
        when(userMapper.findByUsername("zhangsan")).thenReturn(dbUser);
        when(passwordEncoder.matches("123456", "123456")).thenReturn(false); // BCrypt 不匹配

        User input = new User();
        input.setUsername("zhangsan");
        input.setPassword("123456");

        Result<User> result = userService.login(input);

        assertEquals(200, result.getCode());
        assertTrue(result.getMsg().contains("登录成功"));
    }

    @Test
    @DisplayName("登录失败：账号不存在")
    void shouldFailWhenAccountNotFound() {
        when(userMapper.findByUsername("ghost")).thenReturn(null);

        User input = new User();
        input.setUsername("ghost");
        input.setPassword("123456");

        Result<User> result = userService.login(input);

        assertEquals(500, result.getCode());
        assertEquals("登录失败：账号不存在！", result.getMsg());
    }

    @Test
    @DisplayName("登录失败：密码错误")
    void shouldFailWhenPasswordWrong() {
        when(userMapper.findByUsername("zhangsan")).thenReturn(dbUser);
        when(passwordEncoder.matches("wrongpass", dbUser.getPassword())).thenReturn(false);

        User input = new User();
        input.setUsername("zhangsan");
        input.setPassword("wrongpass");

        Result<User> result = userService.login(input);

        assertEquals(500, result.getCode());
        assertEquals("登录失败：密码错误！", result.getMsg());
    }

    @Test
    @DisplayName("登录失败：账号已被封禁")
    void shouldFailWhenAccountBanned() {
        dbUser.setStatus(0); // 封禁状态
        when(userMapper.findByUsername("zhangsan")).thenReturn(dbUser);
        when(passwordEncoder.matches("123456", dbUser.getPassword())).thenReturn(true);

        User input = new User();
        input.setUsername("zhangsan");
        input.setPassword("123456");

        Result<User> result = userService.login(input);

        assertEquals(500, result.getCode());
        assertEquals("登录失败：您的账号已被封禁！", result.getMsg());
    }

    // ==================== 用户维护测试 ====================

    @Test
    @DisplayName("更新资料成功：昵称去除首尾空白且密码不返回")
    void shouldUpdateProfileSuccessfully() {
        when(userMapper.findById(2)).thenReturn(dbUser);
        when(userMapper.updateProfile(2, "新昵称")).thenReturn(1);

        Result<User> result = userService.updateProfile(2, "  新昵称  ");

        assertEquals(200, result.getCode());
        assertNull(result.getData().getPassword());
        verify(userMapper).updateProfile(2, "新昵称");
    }

    @Test
    @DisplayName("更新状态失败：拒绝 0 和 1 以外的值")
    void shouldRejectInvalidUserStatus() {
        Result<Object> result = userService.updateUserStatus(2, 3);

        assertEquals(500, result.getCode());
        assertEquals("用户状态只能是 0 或 1", result.getMsg());
        verify(userMapper, never()).updateStatus(anyInt(), anyInt());
    }

    @Test
    @DisplayName("删除失败：用户不存在时不执行删除")
    void shouldNotDeleteMissingUser() {
        when(userMapper.findById(404)).thenReturn(null);

        Result<Object> result = userService.deleteUser(404);

        assertEquals(500, result.getCode());
        assertEquals("用户不存在", result.getMsg());
        verify(userMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("修改密码成功：校验旧密码并保存 BCrypt 结果")
    void shouldChangePasswordSuccessfully() {
        when(userMapper.getPasswordById(2)).thenReturn(dbUser.getPassword());
        when(passwordEncoder.matches("123456", dbUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("$2a$10$newHash");
        when(userMapper.updatePassword(2, "$2a$10$newHash")).thenReturn(1);

        Result<Object> result = userService.changePassword(2, "123456", "newpass123");

        assertEquals(200, result.getCode());
        verify(userMapper).updatePassword(2, "$2a$10$newHash");
    }
}
