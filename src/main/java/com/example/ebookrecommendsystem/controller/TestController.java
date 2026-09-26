package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.entity.User;
import com.example.ebookrecommendsystem.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
public class TestController {

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/test")
    public String testDB() {
        User adminUser = userMapper.findByUsername("admin");

        if (adminUser != null) {
            return "数据库连接完美！查到的管理员昵称是：" + adminUser.getNickname();
        } else {
            return "查不到数据，请检查数据库里有没有 admin 账号";
        }
    }
}