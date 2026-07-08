package com.convergeai.web;

import com.convergeai.ai.LlmRouter;
import com.convergeai.config.OpenRouterProperties;
import com.convergeai.dto.ConfigStatusDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final OpenRouterProperties openRouterProperties;
    private final LlmRouter llmRouter;

    public ConfigController(OpenRouterProperties openRouterProperties, LlmRouter llmRouter) {
        this.openRouterProperties = openRouterProperties;
        this.llmRouter = llmRouter;
    }

    @GetMapping
    public ConfigStatusDto status() {
        return ConfigStatusDto.from(openRouterProperties, llmRouter);
    }
}
