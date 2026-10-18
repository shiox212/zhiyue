package com.example.ebookrecommendsystem.ai;

public interface LanguageModel {
    record Generation(String text, String requestId, String model, long inputTokens, long outputTokens) {}
    boolean ready();
    Generation generate(String system, String user);
}
