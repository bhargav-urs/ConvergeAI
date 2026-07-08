package com.convergeai.service.debate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SummarizeCauseTest {

    @Test
    void extractsRawReasonFromOpenRouterJsonBlob() {
        String message = "openrouter returned HTTP 429 for model openai/gpt-oss-120b:free: "
                + "{\"error\":{\"message\":\"Provider returned error\",\"code\":429,\"metadata\":"
                + "{\"raw\":\"openai/gpt-oss-120b:free is temporarily rate-limited upstream. "
                + "Please retry shortly, or add your own key to accumulate your rate limits: https://x\","
                + "\"provider_name\":\"OpenInference\"}}}";

        String summary = DebateOrchestrator.summarizeCause(message);

        assertThat(summary)
                .contains("temporarily rate-limited upstream")
                .doesNotContain("Please retry shortly")
                .doesNotContain("{\"error\"");
    }

    @Test
    void keepsPlainMessagesUntouched() {
        assertThat(DebateOrchestrator.summarizeCause(
                "OpenRouter daily free-model limit reached for this key (resets at midnight UTC)."))
                .startsWith("OpenRouter daily free-model limit reached");
    }

    @Test
    void truncatesLongPlainMessages() {
        String longMessage = "x".repeat(400);
        assertThat(DebateOrchestrator.summarizeCause(longMessage)).hasSizeLessThanOrEqualTo(201);
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(DebateOrchestrator.summarizeCause(null)).isEqualTo("Unknown error");
        assertThat(DebateOrchestrator.summarizeCause("  ")).isEqualTo("Unknown error");
    }
}
