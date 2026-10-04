package com.example.ebookrecommendsystem.controller;

import com.example.ebookrecommendsystem.ai.*;
import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.security.Identity;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class LearningAssistantController {
    public record Question(@NotBlank @Size(max=1000) String question) {}
    public record Status(boolean configured, String provider, String model, int documents, int chunks) {}
    private final LearningAssistant assistant;
    private final LanguageModel model;
    private final KnowledgeBase knowledge;
    private final AiSettings settings;

    public LearningAssistantController(LearningAssistant assistant, LanguageModel model, KnowledgeBase knowledge, AiSettings settings) {
        this.assistant = assistant; this.model = model; this.knowledge = knowledge; this.settings = settings;
    }
    @GetMapping("/status") public Result<Status> status(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Result.success(new Status(model.ready(), "DeepSeek", settings.model(), knowledge.documentCount(), knowledge.chunks().size()), "学习助手状态");
    }
    @PostMapping("/ask") public Result<LearningAssistant.Answer> ask(@RequestBody @Valid Question input, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Result.success(assistant.ask(Identity.currentId(), input.question()), "学习助手已处理");
    }
}
