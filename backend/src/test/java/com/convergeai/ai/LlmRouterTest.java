package com.convergeai.ai;

import com.convergeai.config.DirectLlmProperties;
import com.convergeai.config.OpenRouterProperties;
import com.convergeai.domain.AgentName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LlmRouterTest {

    private static OpenRouterProperties openRouter(String apiKey) {
        return new OpenRouterProperties(
                apiKey, "https://openrouter.ai/api/v1", "ref", "ConvergeAI",
                180, 3, 0.3, 4096,
                new OpenRouterProperties.Models("or-analyst", "or-engineer", "or-reviewer", "or-consensus"),
                List.of("fallback-1"));
    }

    private static DirectLlmProperties direct(String groqKey) {
        return new DirectLlmProperties(
                Map.of("groq", new DirectLlmProperties.Provider("https://api.groq.com/openai/v1", groqKey)),
                Map.of(
                        "reviewer", new DirectLlmProperties.Route("groq", "llama-3.3-70b-versatile"),
                        "consensus", new DirectLlmProperties.Route("groq", "llama-3.3-70b-versatile")));
    }

    @Test
    void directProviderComesFirstWhenConfigured() {
        LlmRouter router = new LlmRouter(direct("gsk-key"), openRouter("sk-or-key"));

        List<LlmEndpoint> chain = router.routesFor(AgentName.REVIEWER);

        assertThat(chain).hasSize(2);
        assertThat(chain.get(0).provider()).isEqualTo("groq");
        assertThat(chain.get(0).openRouterExtensions()).isFalse();
        assertThat(chain.get(1).provider()).isEqualTo("openrouter");
        assertThat(chain.get(1).model()).isEqualTo("or-reviewer");
        assertThat(chain.get(1).openRouterExtensions()).isTrue();
    }

    @Test
    void missingDirectKeyFallsBackToOpenRouterOnly() {
        LlmRouter router = new LlmRouter(direct(""), openRouter("sk-or-key"));

        List<LlmEndpoint> chain = router.routesFor(AgentName.REVIEWER);

        assertThat(chain).hasSize(1);
        assertThat(chain.getFirst().provider()).isEqualTo("openrouter");
    }

    @Test
    void agentWithoutDirectRouteUsesOpenRouter() {
        LlmRouter router = new LlmRouter(direct("gsk-key"), openRouter("sk-or-key"));

        List<LlmEndpoint> chain = router.routesFor(AgentName.ANALYST);

        assertThat(chain).hasSize(1);
        assertThat(chain.getFirst().model()).isEqualTo("or-analyst");
    }

    @Test
    void consensusRouteResolves() {
        LlmRouter router = new LlmRouter(direct("gsk-key"), openRouter("sk-or-key"));

        List<LlmEndpoint> chain = router.routesForConsensus();

        assertThat(chain).hasSize(2);
        assertThat(chain.get(0).label()).isEqualTo("groq:llama-3.3-70b-versatile");
        assertThat(chain.get(1).model()).isEqualTo("or-consensus");
    }

    @Test
    void reportsConfiguredState() {
        assertThat(new LlmRouter(direct(""), openRouter("")).isAnyProviderConfigured()).isFalse();
        assertThat(new LlmRouter(direct("gsk"), openRouter("")).isAnyProviderConfigured()).isTrue();
        assertThat(new LlmRouter(direct(""), openRouter("sk")).isAnyProviderConfigured()).isTrue();
        assertThat(new LlmRouter(direct("gsk"), openRouter("sk")).configuredDirectProviders())
                .containsExactly("groq");
    }
}
