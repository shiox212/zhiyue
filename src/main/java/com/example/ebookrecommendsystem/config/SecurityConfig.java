package com.example.ebookrecommendsystem.config;
import com.example.ebookrecommendsystem.security.*;
import com.example.ebookrecommendsystem.mapper.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.http.HttpMethod;
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecuritySettings.class)
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type=org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
    public SecurityFilterChain filterChain(HttpSecurity http,JwtTokens tokens,UserMapper users,SecurityMapper security,SecuritySettings settings) throws Exception {
        var csrf=new CookieCsrfTokenRepository();
        csrf.setCookieCustomizer(cookie->cookie.path("/").httpOnly(true).secure(settings.secureCookies()).sameSite("Strict"));
        http.cors(Customizer.withDefaults())
            .csrf(config->config.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .sessionManagement(config->config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(config->config.disable())
            .authorizeHttpRequests(auth->auth
                .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/session","/api/session/refresh","/api/user/register").permitAll()
                .requestMatchers(HttpMethod.DELETE,"/api/session").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/csrf","/api/books","/api/books/**","/api/book/list","/api/book/search",
                    "/api/book/categories","/api/book/categories-with-count","/api/book/ranking","/api/book/similar","/api/book/rating-distribution","/api/review/list").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/","/*.html","/favicon.ico","/css/**","/js/**","/img/**","/fonts/**","/error","/swagger-ui/**","/v3/api-docs/**").permitAll()
                .anyRequest().denyAll())
            .exceptionHandling(config->config
                .authenticationEntryPoint((req,res,e)->{res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"code\":401,\"msg\":\"请先登录或重新登录\",\"data\":null}");})
                .accessDeniedHandler((req,res,e)->{res.setStatus(403);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"code\":403,\"msg\":\"无权访问或安全校验失败\",\"data\":null}");}))
            .formLogin(config->config.disable()).httpBasic(config->config.disable()).logout(config->config.disable())
            .addFilterBefore(new JwtCookieFilter(tokens,users,security),UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
