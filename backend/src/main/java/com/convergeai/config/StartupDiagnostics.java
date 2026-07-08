package com.convergeai.config;

import com.convergeai.ai.LlmRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Logs the effective AI configuration at startup so misconfiguration is
 * impossible to miss in the logs (the #1 support question is a missing key).
 */
@Component
public class StartupDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupDiagnostics.class);

    private final OpenRouterProperties openRouterProperties;
    private final LlmRouter llmRouter;

    public StartupDiagnostics(OpenRouterProperties openRouterProperties, LlmRouter llmRouter) {
        this.openRouterProperties = openRouterProperties;
        this.llmRouter = llmRouter;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> direct = llmRouter.configuredDirectProviders();
        log.info("Direct fast providers configured: {}", direct.isEmpty() ? "none" : direct);
        log.info("OpenRouter configured: {} (fallback models — analyst: {}, engineer: {}, reviewer: {}, consensus: {})",
                openRouterProperties.isConfigured(),
                openRouterProperties.models().analyst(),
                openRouterProperties.models().engineer(),
                openRouterProperties.models().reviewer(),
                openRouterProperties.models().consensus());
        if (!llmRouter.isAnyProviderConfigured()) {
            log.warn("╔══════════════════════════════════════════════════════════════════════╗");
            log.warn("║  No LLM provider is configured.                                       ║");
            log.warn("║  Document upload/indexing works, but every debate will be rejected.   ║");
            log.warn("║  Set OPENROUTER_API_KEY (https://openrouter.ai/keys) — and optionally ║");
            log.warn("║  GROQ_API_KEY / CEREBRAS_API_KEY / GEMINI_API_KEY for faster debates. ║");
            log.warn("╚══════════════════════════════════════════════════════════════════════╝");
        } else if (direct.isEmpty()) {
            log.warn("No direct fast-provider keys set (GROQ_API_KEY / CEREBRAS_API_KEY / GEMINI_API_KEY). "
                    + "Debates will use the slower OpenRouter free pool.");
        }
    }
}
