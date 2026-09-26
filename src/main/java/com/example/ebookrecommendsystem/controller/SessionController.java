package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.User;
import com.example.ebookrecommendsystem.service.UserService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final UserService userService;

    public SessionController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ApiResponse(responseCode = "200", description = "登录成功", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "账号、密码错误或账号不可用", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class), examples = @ExampleObject(value = "{\"code\":401,\"msg\":\"认证失败\",\"data\":null}")))
    public ResponseEntity<Result<User>> login(@RequestBody @Valid User credentials,
                                               HttpServletRequest request) {
        Result<User> result = userService.login(credentials);
        if (result.getCode() != 200) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(HttpStatus.UNAUTHORIZED.value(), result.getMsg()));
        }

        HttpSession current = request.getSession(false);
        if (current != null) {
            current.invalidate();
        }
        request.getSession(true).setAttribute("loginUser", result.getData());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "返回当前用户", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "当前会话未登录", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class), examples = @ExampleObject(value = "{\"code\":401,\"msg\":\"当前会话未登录\",\"data\":null}")))
    public ResponseEntity<Result<User>> current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("loginUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(HttpStatus.UNAUTHORIZED.value(), "当前会话未登录"));
        }
        user.setPassword(null);
        return ResponseEntity.ok(Result.success(user, "获取当前用户成功"));
    }

    @DeleteMapping
    public Result<Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.success(null, "已安全退出");
    }
}
