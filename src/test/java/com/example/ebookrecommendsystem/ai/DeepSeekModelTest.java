package com.example.ebookrecommendsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class DeepSeekModelTest {
    HttpServer server;
    DeepSeekModel model;
    final ObjectMapper json = new ObjectMapper();
    final AtomicReference<String> request = new AtomicReference<>();
    final AtomicReference<String> authorization = new AtomicReference<>();
    final AtomicInteger calls = new AtomicInteger();
    int status = 200;
    String finish = "stop";
    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet(); request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = (status == 200 ? json.writeValueAsString(Map.of("id", "fixture-request", "object", "chat.completion", "created", 1,
                    "model", "deepseek-flash", "choices", List.of(Map.of("index", 0, "finish_reason", finish, "message", Map.of("role", "assistant", "content", "{\"status\":\"insufficient\"}"))),
                    "usage", Map.of("prompt_tokens", 12, "completion_tokens", 8, "total_tokens", 20)))
                    : "{\"error\":{\"message\":\"upstream-secret-fixture\"}}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json"); exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        model = new DeepSeekModel(OpenAIOkHttpClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("fixture-key").maxRetries(0).timeout(Duration.ofSeconds(5)).build(), "deepseek-flash");
    }
    @AfterEach void stop() { if (model != null) model.close(); if (server != null) server.stop(0); }
    @Test void sdkSendsCompatibleContractAndReadsUsage() throws Exception {
        var result = model.generate("system rules json", "user data");
        var sent = json.readTree(request.get());
        assertEquals("Bearer fixture-key", authorization.get());
        assertEquals("deepseek-flash", sent.path("model").asText());
        assertEquals("system", sent.path("messages").get(0).path("role").asText());
        assertEquals("user", sent.path("messages").get(1).path("role").asText());
        assertEquals("disabled", sent.path("thinking").path("type").asText());
        assertEquals("json_object", sent.path("response_format").path("type").asText());
        assertEquals(1200, sent.path("max_tokens").asInt()); assertEquals(12, result.inputTokens());
        assertEquals("fixture-request", result.requestId());
    }
    @Test void providerErrorsAreSanitizedAndNotRetried() {
        status = 401;
        var error = assertThrows(AiServiceException.class, () -> model.generate("json", "question"));
        assertEquals(503, error.status()); assertFalse(error.getMessage().contains("upstream-secret"));
        assertEquals(1, calls.get());
    }
    @Test void truncatedAnswerIsRejected() {
        finish = "length";
        assertEquals(502, assertThrows(AiServiceException.class, () -> model.generate("json", "question")).status());
    }
    @Test void absentKeyDoesNotConstructOnlineClient() {
        var disabled = new DeepSeekModel(new AiSettings("", "deepseek-flash"));
        assertFalse(disabled.ready());
        assertThrows(AiServiceException.class, () -> disabled.generate("system", "user"));
        assertEquals(0, calls.get());
    }
}
