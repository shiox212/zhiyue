package com.example.ebookrecommendsystem.controller;
import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.User;
import com.example.ebookrecommendsystem.service.UserService;
import com.example.ebookrecommendsystem.security.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/user")
public class UserController {
    public record Registration(@NotBlank @Size(min=3,max=20) String username,@NotBlank @Size(min=6,max=20) String password,@Size(max=20) String nickname) {}
    public record Profile(Integer userId,@NotBlank @Size(max=20) String nickname) {}
    public record PasswordChange(Integer userId,@NotBlank String oldPassword,@NotBlank @Size(min=6,max=20) String newPassword) {}
    private final UserService users;
    private final AccountSessions sessions;
    private final TokenCookies cookies;
    public UserController(UserService users,AccountSessions sessions,TokenCookies cookies){this.users=users;this.sessions=sessions;this.cookies=cookies;}
    @PostMapping("/register") public Result<Object> register(@RequestBody @Valid Registration input) {
        User user=new User();user.setUsername(input.username());user.setPassword(input.password());user.setNickname(input.nickname());
        return sessions.register(user);
    }
    @PostMapping("/profile/update") public Result<User> updateProfile(@RequestBody @Valid Profile input) {
        return users.updateProfile(Identity.own(input.userId()),input.nickname());
    }
    @PostMapping("/password/change") public Result<Object> changePassword(@RequestBody @Valid PasswordChange input,HttpServletResponse response) {
        var result=sessions.changePassword(Identity.own(input.userId()),input.oldPassword(),input.newPassword());
        if(result.getCode()==200)cookies.clear(response);
        return result;
    }
    @GetMapping("/stats") public Result<Map<String,Object>> stats(@RequestParam(required=false) Integer userId) {
        return users.getUserStats(Identity.own(userId));
    }
}
