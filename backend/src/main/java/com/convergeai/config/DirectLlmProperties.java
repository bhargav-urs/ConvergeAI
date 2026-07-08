package com.convergeai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Direct (non-broker) LLM providers with free tiers on fast hardware —
 * Groq, Cerebras, Google AI Studio. All expose OpenAI-compatible
 * /chat/completions endpoints, so the same client speaks to all of them.
 *
 * <p>Each agent has one direct route; the router appends the OpenRouter free
 * pool as the terminal fallback, so a missing key or a failing provider never
 * breaks a debate — it just gets slower.</p>
 */
@ConfigurationProperties(prefix = "llm.direct")
public record DirectLlmProperties(
        /** provider name → endpoint config (e.g. "groq", "cerebras", "gemini"). */
        Map<String, Provider> providers,
        /** route key ("analyst", "engineer", "reviewer", "consensus") → direct route. */
        Map<String, Route> routes
) {

    public record Provider(String baseUrl, String apiKey) {

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Route(String provider, String model) {
    }

    public Provider provider(String name) {
        return providers == null ? null : providers.get(name);
    }

    public Route route(String key) {
        return routes == null ? null : routes.get(key);
    }
}
