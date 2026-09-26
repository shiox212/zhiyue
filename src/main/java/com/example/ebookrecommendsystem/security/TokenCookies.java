package com.example.ebookrecommendsystem.security;

import jakarta.servlet.http.*;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class TokenCookies {
    public static final String ACCESS = "ZY_ACCESS";
    public static final String REFRESH = "ZY_REFRESH";
    private final SecuritySettings settings;
    public TokenCookies(SecuritySettings settings) { this.settings=settings; }
    public static String read(HttpServletRequest request, String name) {
        if(request.getCookies()!=null) for(Cookie c:request.getCookies()) if(c.getName().equals(name)) return c.getValue();
        return null;
    }
    public void set(HttpServletResponse response, String access, String refresh) {
        write(response, ACCESS, access, "/api", settings.accessSeconds());
        write(response, REFRESH, refresh, "/api/session", settings.refreshSeconds());
    }
    public void clear(HttpServletResponse response) {
        write(response, ACCESS, "", "/api", 0); write(response, REFRESH, "", "/api/session", 0);
    }
    private void write(HttpServletResponse response,String name,String value,String path,int age) {
        response.addHeader("Set-Cookie",ResponseCookie.from(name,value).httpOnly(true).secure(settings.secureCookies())
                .sameSite("Strict").path(path).maxAge(age).build().toString());
        response.setHeader("Cache-Control","no-store");
    }
}
