package com.convergeai.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model,
        /**
         * OpenRouter fallback routing: ordered list of models tried when the
         * primary is unavailable/rate-limited (API caps this at 3 entries).
         * The response reports which model actually served the completion.
         */
        List<String> models,
        List<ChatMessage> messages,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens,
        /**
         * OpenRouter unified reasoning controls. Low effort keeps reasoning
         * models (Nemotron, GPT-OSS, R1-style) from burning the whole token
         * budget on chain-of-thought before the actual answer. Ignored by
         * non-reasoning models.
         */
        Reasoning reasoning
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Reasoning(String effort) {
        public static Reasoning low() {
            return new Reasoning("low");
        }
    }
}
