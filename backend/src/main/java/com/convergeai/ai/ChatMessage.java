package com.convergeai.ai;

/**
 * OpenAI-compatible chat message as accepted by the OpenRouter /chat/completions API.
 */
public record ChatMessage(String role, String content) {

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }
}
