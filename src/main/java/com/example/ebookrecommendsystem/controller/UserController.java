package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.User;
import com.example.ebookrecommendsystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<Object> register(@RequestBody @Valid User user) {
        return userService.register(user);
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody @Valid User user, HttpSession session, HttpServletRequest request) {
        Result<User> result = userService.login(user);
        if (result.getCode() == 200) {
            session.invalidate();
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute("loginUser", result.getData());
        }
        return result;
    }

    @PostMapping("/profile/update")
    public Result<User> updateProfile(@RequestBody Map<String, Object> params, HttpSession session) {
        Integer id = (Integer) params.get("userId");
        String nickname = (String) params.get("nickname");
        Result<User> result = userService.updateProfile(id, nickname);
        if (result.getCode() == 200) {
            session.setAttribute("loginUser", result.getData());
        }
        return result;
    }

    @PostMapping("/password/change")
    public Result<Object> changePassword(@RequestBody Map<String, Object> params, HttpSession session) {
        Integer id = (Integer) params.get("userId");
        String oldPassword = (String) params.get("oldPassword");
        String newPassword = (String) params.get("newPassword");
        Result<Object> result = userService.changePassword(id, oldPassword, newPassword);
        if (result.getCode() == 200) {
            session.invalidate();
        }
        return result;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam Integer userId) {
        return userService.getUserStats(userId);
    }
}