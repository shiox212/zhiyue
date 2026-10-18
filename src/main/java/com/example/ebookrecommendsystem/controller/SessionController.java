package com.example.ebookrecommendsystem.controller;
import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.entity.User;
import com.example.ebookrecommendsystem.security.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
@RestController
public class SessionController {
    public record Credentials(@NotBlank @Size(min=3,max=20) String username,@NotBlank @Size(min=6,max=20) String password) {}
    private final AccountSessions sessions;
    private final TokenCookies cookies;
    public SessionController(AccountSessions sessions,TokenCookies cookies){this.sessions=sessions;this.cookies=cookies;}
    @GetMapping("/api/csrf")
    public Result<java.util.Map<String,String>> csrf(CsrfToken token,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        return Result.success(java.util.Map.of("token",token.getToken()),"安全校验已就绪");
    }
    @PostMapping("/api/session")
    @ApiResponse(responseCode="200",description="登录成功，凭据写入 HttpOnly Cookie")
    @ApiResponse(responseCode="401",description="认证失败")
    public Result<User> login(@RequestBody @Valid Credentials input,HttpServletResponse response) {
        User credentials=new User();credentials.setUsername(input.username());credentials.setPassword(input.password());
        var result=sessions.login(credentials);cookies.set(response,result.access(),result.refresh());
        return Result.success(result.user(),"登录成功");
    }
    @GetMapping("/api/session")
    @ApiResponse(responseCode="200",description="当前用户")
    @ApiResponse(responseCode="401",description="未登录或登录已过期")
    public Result<User> current(HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");return Result.success(sessions.current(),"获取当前用户成功");
    }
    @PostMapping("/api/session/refresh")
    public Result<User> refresh(HttpServletRequest request,HttpServletResponse response) {
        var result=sessions.refresh(TokenCookies.read(request,TokenCookies.REFRESH));
        cookies.set(response,result.access(),result.refresh());return Result.success(result.user(),"登录已续期");
    }
    @DeleteMapping("/api/session")
    public Result<Object> logout(Authentication auth,HttpServletRequest request,HttpServletResponse response) {
        Integer id=auth!=null && auth.getPrincipal() instanceof Identity p?p.userId():null;
        sessions.logout(id,TokenCookies.read(request,TokenCookies.REFRESH));cookies.clear(response);
        return Result.success(null,"已退出全部登录设备");
    }
}
