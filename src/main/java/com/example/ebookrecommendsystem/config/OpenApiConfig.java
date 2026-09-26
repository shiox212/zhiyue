package com.example.ebookrecommendsystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI zhiyueOpenApi() {
        return new OpenAPI().info(new Info()
                .title("知阅 REST API")
                .version("1.0.0")
                .description("实验二：Vue 前端与 Spring Boot 后端分离接口"));
    }
}
