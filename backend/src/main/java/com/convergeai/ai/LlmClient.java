package com.convergeai.ai;

import com.convergeai.config.OpenRouterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Blocking chat-completion client for any OpenAI-compatible endpoint (Groq,
 * Cerebras, Google AI Studio, OpenRouter). Callers run on virtual threads, so
 * blocking here is cheap.
 *
 * <p>Resilience model: the caller passes an ordered {@link LlmEndpoint} chain.
 * Transient failures (429/5xx/timeouts) are retried per endpoint — honoring the
 * server's {@code Retry-After} when present — and when an endpoint is exhausted
 * the client fails over to the next one in the chain.</p>
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    /** R1-style inline chain-of-thought, stripped from displayed answers. */
    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?</think>");
    /** OpenRouter embeds the upstream cool-down in the 429 error body. */
    private static final Pattern RETRY_AFTER_SECONDS = Pattern.compile("\"retry_after_seconds\"\\s*:\\s*(\\d+)");

    private static final long BASE_BACKOFF_MILLIS = 2_000;
    private static final long MAX_BACKOFF_MILLIS = 45_000;
    /** OpenRouter rejects routing lists longer than 3 models. */
    private static final int MAX_ROUTING_MODELS = 3;
    /** Non-terminal endpoints get a short retry budget before failing over. */
    private static final int NON_TERMINAL_ATTEMPTS = 2;

    private final RestClient restClient;
    private final OpenRouterProperties properties;

    /** A completed chat call: the text plus which provider/model served it. */
    public record CompletionResult(String content, String servedBy) {
    }

    public LlmClient(OpenRouterProperties properties) {
        this.properties = properties;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.requestTimeoutSeconds()));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Executes a chat completion against the first endpoint in the chain that
     * succeeds, with retries and cross-provider failover.
     *
     * @param maxTokensOverride optional per-call output budget (fast mode);
     *                          {@code null} uses the configured default
     * @throws OpenRouterException when every endpoint in the chain is exhausted
     */
    public CompletionResult complete(List<LlmEndpoint> chain,
                                     List<ChatMessage> messages,
                                     Integer maxTokensOverride) {
        if (chain == null || chain.isEmpty()) {
            throw new OpenRouterException(
                    "No LLM provider is configured. Set OPENROUTER_API_KEY (or a direct provider key) "
                            + "in the backend environment.", false);
        }
        OpenRouterException lastFailure = null;
        for (int i = 0; i < chain.size(); i++) {
            LlmEndpoint endpoint = chain.get(i);
            boolean isLast = i == chain.size() - 1;
            try {
                return completeOnEndpoint(endpoint, messages, maxTokensOverride, isLast);
            } catch (OpenRouterException e) {
                lastFailure = e;
                if (!isLast) {
                    log.warn("Endpoint {} exhausted ({}); failing over to {}",
                            endpoint.label(), e.getMessage(), chain.get(i + 1).label());
                }
            }
        }
        throw lastFailure;
    }

    private CompletionResult completeOnEndpoint(LlmEndpoint endpoint,
                                                List<ChatMessage> messages,
                                                Integer maxTokensOverride,
                                                boolean isTerminalEndpoint) {
        ChatCompletionRequest request = buildRequest(endpoint, messages, maxTokensOverride);
        int maxAttempts = isTerminalEndpoint
                ? Math.max(1, properties.maxRetries() + 1)
                : NON_TERMINAL_ATTEMPTS;

        OpenRouterException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeOnce(endpoint, request);
            } catch (OpenRouterException e) {
                lastFailure = e;
                if (!e.isRetryable() || attempt == maxAttempts) {
                    throw e;
                }
                sleepBeforeRetry(attempt, endpoint.label(), e);
            }
        }
        throw lastFailure;
    }

    private ChatCompletionRequest buildRequest(LlmEndpoint endpoint,
                                               List<ChatMessage> messages,
                                               Integer maxTokensOverride) {
        int maxTokens = maxTokensOverride != null ? maxTokensOverride : properties.maxTokens();
        if (endpoint.openRouterExtensions()) {
            return new ChatCompletionRequest(
                    endpoint.model(), routingListFor(endpoint.model()), messages,
                    properties.temperature(), maxTokens,
                    ChatCompletionRequest.Reasoning.low());
        }
        // Plain OpenAI-compatible request: unknown fields can be rejected by
        // strict providers, so no broker-specific extensions.
        return new ChatCompletionRequest(
                endpoint.model(), null, messages, properties.temperature(), maxTokens, null);
    }

    /** Primary model first, then the configured fallbacks (deduplicated, capped at the API limit). */
    private List<String> routingListFor(String primaryModel) {
        List<String> fallbacks = properties.fallbackModels();
        if (fallbacks == null || fallbacks.isEmpty()) {
            return null;
        }
        List<String> routing = new ArrayList<>();
        routing.add(primaryModel);
        for (String fallback : fallbacks) {
            if (routing.size() >= MAX_ROUTING_MODELS) {
                break;
            }
            if (StringUtils.hasText(fallback) && !routing.contains(fallback.strip())) {
                routing.add(fallback.strip());
            }
        }
        return routing.size() > 1 ? routing : null;
    }

    private CompletionResult executeOnce(LlmEndpoint endpoint, ChatCompletionRequest request) {
        ChatCompletionResponse response;
        try {
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(endpoint.baseUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + endpoint.apiKey());
            if (endpoint.openRouterExtensions()) {
                // OpenRouter attribution headers (recommended for free-tier usage).
                spec = spec.header("HTTP-Referer", properties.referer())
                        .header("X-Title", properties.appTitle());
            }
            response = spec.body(request).retrieve().body(ChatCompletionResponse.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            // A spent daily quota will not recover within any retry window —
            // retrying just burns a minute per agent before the same failure.
            if (body != null && body.contains("free-models-per-day")) {
                throw new OpenRouterException(
                        "OpenRouter daily free-model limit reached for this key (resets at midnight UTC). "
                                + "Add GROQ_API_KEY / CEREBRAS_API_KEY / GEMINI_API_KEY (free, separate quotas) "
                                + "or $10 of OpenRouter credits to lift the cap.",
                        false, null, e);
            }
            boolean retryable = status == 408 || status == 429 || status >= 500;
            throw new OpenRouterException(
                    "%s returned HTTP %d for model %s: %s"
                            .formatted(endpoint.provider(), status, request.model(), truncate(body, 400)),
                    retryable, extractRetryAfterMillis(e, body), e);
        } catch (ResourceAccessException e) {
            throw new OpenRouterException(
                    "I/O error calling %s for model %s: %s"
                            .formatted(endpoint.provider(), request.model(), e.getMessage()),
                    true, e);
        }

        if (response == null) {
            throw new OpenRouterException(
                    "Empty response from " + endpoint.provider() + " for model " + request.model(), true);
        }
        if (response.error() != null) {
            Integer code = response.error().code();
            boolean retryable = code == null || code == 408 || code == 429 || code >= 500;
            throw new OpenRouterException(
                    "%s error for model %s (code %s): %s"
                            .formatted(endpoint.provider(), request.model(), code, response.error().message()),
                    retryable);
        }
        if (response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst().message() == null) {
            throw new OpenRouterException(
                    "No choices returned by " + endpoint.provider() + " model " + request.model(), true);
        }

        ChatCompletionResponse.Message message = response.choices().getFirst().message();
        String content = sanitize(message.content());
        if (!StringUtils.hasText(content)) {
            // Reasoning models occasionally emit only the reasoning stream when they
            // hit the token cap; the reasoning is better than nothing for the debate.
            content = sanitize(message.reasoning());
        }
        if (!StringUtils.hasText(content)) {
            throw new OpenRouterException(
                    "Blank completion from " + endpoint.provider() + " model " + request.model(), true);
        }

        String servedBy;
        if (endpoint.openRouterExtensions()) {
            // OpenRouter's own fallback routing may serve a different model.
            String servedModel = StringUtils.hasText(response.model()) ? response.model() : request.model();
            servedBy = "openrouter:" + servedModel;
            if (!servedModel.equals(request.model())) {
                log.info("OpenRouter fallback: {} was unavailable, served by {}", request.model(), servedModel);
            }
        } else {
            servedBy = endpoint.label();
        }
        return new CompletionResult(content, servedBy);
    }

    /** Pulls the server-requested cool-down from the Retry-After header or error body. */
    private static Long extractRetryAfterMillis(RestClientResponseException e, String body) {
        String header = e.getResponseHeaders() != null
                ? e.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER)
                : null;
        if (header != null) {
            try {
                return Long.parseLong(header.strip()) * 1000;
            } catch (NumberFormatException ignored) {
                // HTTP-date form; fall through to the body
            }
        }
        if (body != null) {
            Matcher matcher = RETRY_AFTER_SECONDS.matcher(body);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1)) * 1000;
            }
        }
        return null;
    }

    private void sleepBeforeRetry(int attempt, String endpointLabel, OpenRouterException cause) {
        long backoff = Math.min(MAX_BACKOFF_MILLIS, BASE_BACKOFF_MILLIS * (1L << (attempt - 1)));
        if (cause.getRetryAfterMillis() != null) {
            // The server knows its cool-down better than our schedule does.
            backoff = Math.min(MAX_BACKOFF_MILLIS, Math.max(backoff, cause.getRetryAfterMillis()));
        }
        long jitter = ThreadLocalRandom.current().nextLong(1_500);
        long sleepMillis = backoff + jitter;
        log.warn("LLM call to {} failed (attempt {}): {}. Retrying in {} ms",
                endpointLabel, attempt, cause.getMessage(), sleepMillis);
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new OpenRouterException("Interrupted while waiting to retry LLM call", false, ie);
        }
    }

    private static String sanitize(String content) {
        if (content == null) {
            return null;
        }
        String cleaned = THINK_BLOCK.matcher(content).replaceAll("");
        // Glitched free-pool completions occasionally contain NUL bytes, which
        // PostgreSQL text columns reject ("invalid byte sequence for encoding UTF8").
        cleaned = cleaned.replace("\u0000", "");
        return cleaned.strip();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }
}
