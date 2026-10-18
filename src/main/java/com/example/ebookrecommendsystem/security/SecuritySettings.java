package com.example.ebookrecommendsystem.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecuritySettings(String jwtSecretBase64, boolean secureCookies, int accessSeconds, int refreshSeconds) {
    public SecuritySettings {
        if (accessSeconds < 1 || accessSeconds > 3600 || refreshSeconds < accessSeconds || refreshSeconds > 2592000)
            throw new IllegalArgumentException("登录有效期配置不正确");
    }
}
