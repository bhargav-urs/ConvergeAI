package com.convergeai.config;

import com.convergeai.domain.AgentName;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * OpenRouter access configuration. Every model is a free-tier model; all four
 * slots are overridable via environment variables so the deployment can swap
 * models without a rebuild (free-tier availability rotates on OpenRouter).
 */
@ConfigurationProperties(prefix = "openrouter")
public record OpenRouterProperties(
        String apiKey,
        @DefaultValue("https://openrouter.ai/api/v1") String baseUrl,
        @DefaultValue("https://github.com/convergeai") String referer,
        @DefaultValue("ConvergeAI") String appTitle,
        @DefaultValue("180") long requestTimeoutSeconds,
        @DefaultValue("3") int maxRetries,
        @DefaultValue("0.3") double temperature,
        @DefaultValue("4096") int maxTokens,
        Models models,
        /**
         * Shared OpenRouter fallback routing list, appended after each agent's
         * primary model. Free-tier availability fluctuates hour to hour; when a
         * primary is rate-limited upstream, OpenRouter transparently serves the
         * first available fallback instead of failing the debate.
         */
        @DefaultValue({
                "nvidia/nemotron-3-super-120b-a12b:free",
                "openai/gpt-oss-20b:free",
                "google/gemma-4-31b-it:free",
                "tencent/hy3:free"
        }) List<String> fallbackModels
) {

    public record Models(
            @DefaultValue("openai/gpt-oss-120b:free") String analyst,
            @DefaultValue("qwen/qwen3-next-80b-a3b-instruct:free") String engineer,
            @DefaultValue("meta-llama/llama-3.3-70b-instruct:free") String reviewer,
            @DefaultValue("meta-llama/llama-3.3-70b-instruct:free") String consensus
    ) {
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String modelFor(AgentName agent) {
        return switch (agent) {
            case ANALYST -> models.analyst();
            case ENGINEER -> models.engineer();
            case REVIEWER -> models.reviewer();
        };
    }
}
