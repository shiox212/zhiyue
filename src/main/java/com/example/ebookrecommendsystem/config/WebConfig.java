package com.example.ebookrecommendsystem.config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfig implements WebMvcConfigurer {
    private final CorsProperties cors;
    public WebConfig(CorsProperties cors){this.cors=cors;}
    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(cors.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                .allowedHeaders("Content-Type","Accept","X-Requested-With","X-XSRF-TOKEN")
                .allowCredentials(true).maxAge(3600);
    }
}
