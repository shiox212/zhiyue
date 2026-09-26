package com.example.ebookrecommendsystem.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> WHITE_LIST = Set.of(
            "/api/user/register",
            "/api/user/login",
            "/api/session",
            "/api/book/list",
            "/api/book/search",
            "/api/book/categories-with-count",
            "/api/book/categories",
            "/api/book/ranking",
            "/api/book/similar",
            "/api/book/rating-distribution",
            "/api/review/list",
            "/test",
            "/",
            "/index.html",
            "/bookshelf.html",
            "/dashboard.html"
    );

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String uri = request.getRequestURI();

        if (HttpMethod.OPTIONS.matches(request.getMethod())
                || WHITE_LIST.contains(uri)
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || (HttpMethod.GET.matches(request.getMethod()) && uri.startsWith("/api/books"))
                || uri.endsWith(".html")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/img/")
                || uri.startsWith("/fonts/")) {
            return true;
        }

        Object loginUser = request.getSession().getAttribute("loginUser");
        if (loginUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\",\"data\":null}");
            return false;
        }

        return true;
    }
}
