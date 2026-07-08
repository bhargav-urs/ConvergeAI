package com.convergeai.dto;

import com.convergeai.ai.LlmRouter;
import com.convergeai.config.OpenRouterProperties;

import java.util.List;

/**
 * Non-sensitive runtime configuration exposed to the frontend so it can warn
 * the user before a debate is doomed (e.g. no LLM provider configured) and
 * hint when fast direct providers are available.
 */
public record ConfigStatusDto(
        boolean anyProviderConfigured,
        boolean openRouterConfigured,
        /** Direct fast providers with keys present (e.g. ["cerebras","gemini","groq"]). */
        List<String> directProviders,
        Models models
) {

    public record Models(String analyst, String engineer, String reviewer, String consensus) {
    }

    public static ConfigStatusDto from(OpenRouterProperties properties, LlmRouter router) {
        return new ConfigStatusDto(
                router.isAnyProviderConfigured(),
                properties.isConfigured(),
                router.configuredDirectProviders(),
                new Models(
                        properties.models().analyst(),
                        properties.models().engineer(),
                        properties.models().reviewer(),
                        properties.models().consensus()));
    }
}
