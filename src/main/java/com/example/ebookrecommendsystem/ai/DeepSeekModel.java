package com.example.ebookrecommendsystem.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Map;

@Component
@EnableConfigurationProperties(AiSettings.class)
public class DeepSeekModel implements LanguageModel {
    private final OpenAIClient client;
    private final String model;

    @Autowired
    public DeepSeekModel(AiSettings settings) {
        this(settings.apiKey().isBlank() ? null : OpenAIOkHttpClient.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(settings.apiKey()).timeout(Duration.ofSeconds(40)).maxRetries(0).build(), settings.model());
    }

    // The second constructor supports a local HTTP fixture in SDK contract tests.
    DeepSeekModel(OpenAIClient client, String model) { this.client = client; this.model = model; }
    @Override public boolean ready() { return client != null; }

    @Override public Generation generate(String system, String user) {
        if (!ready()) throw new AiServiceException(503, "学习助手尚未配置，请联系项目维护者");
        try {
            var params = ChatCompletionCreateParams.builder().model(model)
                    .addSystemMessage(system).addUserMessage(user)
                    .temperature(0.0).maxTokens(1200L)
                    .responseFormat(ResponseFormatJsonObject.builder().build())
                    .putAdditionalBodyProperty("thinking", JsonValue.from(Map.of("type", "disabled")))
                    .build();
            var response = client.chat().completions().create(params);
            if (response.choices().size() != 1 || !response.choices().get(0).finishReason().equals(ChatCompletion.Choice.FinishReason.STOP)) {
                throw new AiServiceException(502, "模型回答不完整，请稍后重试");
            }
            String content = response.choices().get(0).message().content().orElse("");
            if (content.isBlank() || content.length() > 12000) throw new AiServiceException(502, "模型未返回有效回答，请稍后重试");
            return new Generation(content, response.id(), response.model(),
                    response.usage().map(u -> u.promptTokens()).orElse(0L),
                    response.usage().map(u -> u.completionTokens()).orElse(0L));
        } catch (AiServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            // SDK exceptions may carry response bodies and request details. Do not log or forward them.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("模型请求失败，异常类型={}", e.getClass().getSimpleName());
            throw new AiServiceException(503, "模型服务暂时不可用，请稍后重试或联系项目维护者");
        }
    }
    @PreDestroy public void close() { if (client != null) client.close(); }
}
