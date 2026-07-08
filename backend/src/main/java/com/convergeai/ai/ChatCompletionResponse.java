package com.convergeai.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenRouter /chat/completions response. OpenRouter can return HTTP 200 with an
 * {@code error} body (e.g. upstream provider failures on free models), so the
 * error field is modelled explicitly rather than trusted to HTTP status alone.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(
        String id,
        String model,
        List<Choice> choices,
        Usage usage,
        ApiError error
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {
    }

    /**
     * Reasoning models (DeepSeek-R1) return their chain-of-thought in a separate
     * {@code reasoning} field alongside {@code content}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content, String reasoning) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiError(Integer code, String message) {
    }
}
