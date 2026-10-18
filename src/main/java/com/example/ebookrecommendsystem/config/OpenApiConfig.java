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
                .version("1.4.0")
                .description("实验四：认证使用 HttpOnly 的 ZY_ACCESS Cookie；修改操作先 GET /api/csrf，"
                        + "保留 Cookie，并将响应中的 token 放入 X-XSRF-TOKEN 请求头。"
                        + "POST /api/session/refresh 轮换刷新凭据，DELETE /api/session 退出全部设备。"
                        + "管理接口同时检查 ADMIN 角色及对应方法权限；个人接口校验数据归属。"))
                .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("cookieAuth",
                        new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY)
                                .in(io.swagger.v3.oas.models.security.SecurityScheme.In.COOKIE).name("ZY_ACCESS")));
    }
}
