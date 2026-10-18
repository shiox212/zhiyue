package com.example.ebookrecommendsystem.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.ai")
public record AiSettings(@DefaultValue("") String apiKey,
                         @DefaultValue("deepseek-flash") String model) {
    @Override public String toString() { return "AiSettings[model=" + model + ", apiKey=REDACTED]"; }
}
