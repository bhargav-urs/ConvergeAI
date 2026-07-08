package com.convergeai.ai;

import com.convergeai.config.DirectLlmProperties;
import com.convergeai.config.OpenRouterProperties;
import com.convergeai.domain.AgentName;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resolves the ordered endpoint chain for each agent:
 *
 * <ol>
 *   <li>The agent's direct fast provider (Groq / Cerebras / Gemini free tier),
 *       if its API key is configured.</li>
 *   <li>The OpenRouter free pool (with its own internal fallback routing),
 *       if the OpenRouter key is configured.</li>
 * </ol>
 *
 * The debate never depends on any single provider being up or configured.
 */
@Component
public class LlmRouter {

    public static final String CONSENSUS_ROUTE = "consensus";

    private final DirectLlmProperties directProperties;
    private final OpenRouterProperties openRouterProperties;

    public LlmRouter(DirectLlmProperties directProperties, OpenRouterProperties openRouterProperties) {
        this.directProperties = directProperties;
        this.openRouterProperties = openRouterProperties;
    }

    public List<LlmEndpoint> routesFor(AgentName agent) {
        return resolve(agent.name().toLowerCase(Locale.ROOT), openRouterProperties.modelFor(agent));
    }

    public List<LlmEndpoint> routesForConsensus() {
        return resolve(CONSENSUS_ROUTE, openRouterProperties.models().consensus());
    }

    /** True when at least one endpoint (direct or broker) can serve calls. */
    public boolean isAnyProviderConfigured() {
        if (openRouterProperties.isConfigured()) {
            return true;
        }
        return directProperties.providers() != null
                && directProperties.providers().values().stream()
                .anyMatch(DirectLlmProperties.Provider::isConfigured);
    }

    /** Direct providers with a key present, for /api/config and diagnostics. */
    public List<String> configuredDirectProviders() {
        if (directProperties.providers() == null) {
            return List.of();
        }
        return directProperties.providers().entrySet().stream()
                .filter(e -> e.getValue().isConfigured())
                .map(java.util.Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private List<LlmEndpoint> resolve(String routeKey, String openRouterModel) {
        List<LlmEndpoint> chain = new ArrayList<>(2);

        DirectLlmProperties.Route route = directProperties.route(routeKey);
        if (route != null) {
            DirectLlmProperties.Provider provider = directProperties.provider(route.provider());
            if (provider != null && provider.isConfigured()) {
                chain.add(new LlmEndpoint(
                        route.provider(), provider.baseUrl(), provider.apiKey(), route.model(), false));
            }
        }

        if (openRouterProperties.isConfigured()) {
            chain.add(new LlmEndpoint(
                    "openrouter", openRouterProperties.baseUrl(), openRouterProperties.apiKey(),
                    openRouterModel, true));
        }
        return chain;
    }
}
