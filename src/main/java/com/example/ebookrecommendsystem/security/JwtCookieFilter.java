package com.example.ebookrecommendsystem.security;

import com.example.ebookrecommendsystem.mapper.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtException;
import java.io.IOException;
import java.util.ArrayList;

// Created only in the security chain; not also registered as a servlet filter.
public class JwtCookieFilter extends OncePerRequestFilter {
    private final JwtTokens tokens;
    private final UserMapper users;
    private final SecurityMapper security;
    public JwtCookieFilter(JwtTokens tokens,UserMapper users,SecurityMapper security) {
        this.tokens=tokens;this.users=users;this.security=security;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        String token=TokenCookies.read(request,TokenCookies.ACCESS);
        if(token!=null) {
            try {
                var jwt=tokens.verify(token);
                int id=Integer.parseInt(jwt.getSubject());
                Object claim=jwt.getClaim("ver");
                var user=users.findById(id);
                var stored=security.version(id);
                if(user!=null && Integer.valueOf(1).equals(user.getStatus()) && stored!=null
                        && claim instanceof Number version && version.doubleValue()==stored.doubleValue()) {
                    var authorities=new ArrayList<SimpleGrantedAuthority>();
                    security.roles(id).forEach(r->authorities.add(new SimpleGrantedAuthority("ROLE_"+r)));
                    security.permissions(id).forEach(p->authorities.add(new SimpleGrantedAuthority(p)));
                    var context=SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(new UsernamePasswordAuthenticationToken(new Identity(id),null,authorities));
                    SecurityContextHolder.setContext(context);
                }
            } catch(JwtException | IllegalArgumentException e) {
                // Invalid/expired token is anonymous; refresh and public endpoints remain reachable.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request,response);
    }
}
