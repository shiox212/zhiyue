package com.example.ebookrecommendsystem.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

public record Identity(Integer userId) {
    public static int currentId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Identity identity))
            throw new AuthenticationCredentialsNotFoundException("请先登录");
        return identity.userId();
    }
    public static int own(Integer requestedId) {
        int id = currentId();
        if (requestedId != null && requestedId != id) throw new AccessDeniedException("不能操作其他用户的数据");
        return id;
    }
}
