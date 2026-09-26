package com.example.ebookrecommendsystem.service;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.User;
import com.example.ebookrecommendsystem.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> register(User user) {
        user.setUsername(user.getUsername().trim());
        User existUser = userMapper.findByUsername(user.getUsername());
        if (existUser != null) {
            return Result.error("注册失败：该账号已被占用！");
        }

        if (user.getNickname() == null || user.getNickname().isBlank()) {
            user.setNickname("热心读者");
        } else {
            user.setNickname(user.getNickname().trim());
        }

        // 密码加密存储
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (userMapper.insertUser(user) != 1) {
            return Result.error("注册失败：用户数据未写入");
        }
        return Result.success(null, "注册成功！");
    }

    public Result<User> login(User user) {
        User dbUser = userMapper.findByUsername(user.getUsername());

        if (dbUser == null) {
            return Result.error("登录失败：账号不存在！");
        }

        // 优先使用 BCrypt 校验，兼容存量明文密码（回退比对）
        boolean passwordMatch = passwordEncoder.matches(user.getPassword(), dbUser.getPassword())
                || dbUser.getPassword().equals(user.getPassword());

        if (!passwordMatch) {
            return Result.error("登录失败：密码错误！");
        }

        if (dbUser.getStatus() == 0) {
            return Result.error("登录失败：您的账号已被封禁！");
        }

        dbUser.setPassword(null);
        return Result.success(dbUser, "登录成功！欢迎回来，" + dbUser.getNickname());
    }

    public Result<List<User>> getAllUsers() {
        List<User> users = userMapper.findAll();
        users.forEach(u -> u.setPassword(null));
        return Result.success(users, "获取用户列表成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> updateUserStatus(Integer id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("用户状态只能是 0 或 1");
        }
        if (userMapper.findById(id) == null) {
            return Result.error("用户不存在");
        }
        if (userMapper.updateStatus(id, status) != 1) {
            return Result.error("用户状态更新失败");
        }
        return Result.success(null, status == 1 ? "账号已解封" : "账号已封禁");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> deleteUser(Integer id) {
        if (userMapper.findById(id) == null) {
            return Result.error("用户不存在");
        }
        if (userMapper.deleteById(id) != 1) {
            return Result.error("用户删除失败");
        }
        return Result.success(null, "用户删除成功");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<User> updateProfile(Integer id, String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return Result.error("昵称不能为空");
        }
        String normalizedNickname = nickname.trim();
        if (normalizedNickname.length() > 20) {
            return Result.error("昵称长度不能超过20位");
        }
        if (userMapper.findById(id) == null) {
            return Result.error("用户不存在");
        }
        if (userMapper.updateProfile(id, normalizedNickname) != 1) {
            return Result.error("个人资料更新失败");
        }
        User user = userMapper.findById(id);
        if (user != null) user.setPassword(null);
        return Result.success(user, "个人资料已更新");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Object> changePassword(Integer id, String oldPassword, String newPassword) {
        if (oldPassword == null || newPassword == null) {
            return Result.error("当前密码和新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return Result.error("新密码长度需在6-20位之间");
        }
        String dbPassword = userMapper.getPasswordById(id);
        if (dbPassword == null) {
            return Result.error("用户不存在");
        }
        boolean oldMatch = passwordEncoder.matches(oldPassword, dbPassword)
                || dbPassword.equals(oldPassword);
        if (!oldMatch) {
            return Result.error("当前密码不正确");
        }
        if (oldPassword.equals(newPassword)) {
            return Result.error("新密码不能与当前密码相同");
        }
        if (userMapper.updatePassword(id, passwordEncoder.encode(newPassword)) != 1) {
            return Result.error("密码修改失败");
        }
        return Result.success(null, "密码修改成功，请重新登录");
    }

    public Result<java.util.Map<String, Object>> getUserStats(Integer userId) {
        if (userMapper.findById(userId) == null) {
            return Result.error("用户不存在");
        }
        int shelfCount = userMapper.countBookshelfByUserId(userId);
        int ratingCount = userMapper.countRatingsByUserId(userId);
        int reviewCount = userMapper.countReviewsByUserId(userId);
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("shelfCount", shelfCount);
        stats.put("ratingCount", ratingCount);
        stats.put("reviewCount", reviewCount);
        return Result.success(stats, "获取统计数据成功");
    }
}
