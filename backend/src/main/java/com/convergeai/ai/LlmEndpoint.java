package com.convergeai.ai;

/**
 * A concrete place to send one chat completion: provider + endpoint + model.
 *
 * <p>{@code openRouterExtensions} gates the OpenRouter-only request fields
 * (fallback {@code models} array, unified {@code reasoning} controls) — other
 * OpenAI-compatible providers may reject unknown fields.</p>
 */
public record LlmEndpoint(
        String provider,
        String baseUrl,
        String apiKey,
        String model,
        boolean openRouterExtensions
) {

    /** Human-readable identity stored with each response, e.g. "groq:llama-3.3-70b-versatile". */
    public String label() {
        return provider + ":" + model;
    }
}
