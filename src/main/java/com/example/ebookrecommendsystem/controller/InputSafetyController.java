package com.example.ebookrecommendsystem.controller;
import com.example.ebookrecommendsystem.common.Result;
import com.example.ebookrecommendsystem.security.PromptInputPolicy;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/safety")
public class InputSafetyController {
    public record CheckInput(String question,List<PromptInputPolicy.Reference> references) {}
    private final PromptInputPolicy policy;
    public InputSafetyController(PromptInputPolicy policy){this.policy=policy;}
    @PostMapping("/validate")
    public Result<Map<String,Object>> validate(@RequestBody CheckInput input) {
        var prepared=policy.prepare(input.question(),input.references());
        return Result.success(Map.of("accepted",true,"referenceCount",prepared.references().size(),"modelCalled",false),"输入检查通过；尚未调用模型");
    }
}
