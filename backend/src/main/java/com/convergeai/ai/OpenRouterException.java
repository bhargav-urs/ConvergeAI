package com.convergeai.ai;

/**
 * Raised when an OpenRouter call fails after all retries, or returns an
 * unusable payload. {@code retryable} records whether the terminal failure was
 * of a transient class (rate limit, 5xx, timeout).
 */
public class OpenRouterException extends RuntimeException {

    private final boolean retryable;
    /** Server-requested wait before retrying (from a 429 Retry-After), if any. */
    private final Long retryAfterMillis;

    public OpenRouterException(String message, boolean retryable) {
        this(message, retryable, null, null);
    }

    public OpenRouterException(String message, boolean retryable, Throwable cause) {
        this(message, retryable, null, cause);
    }

    public OpenRouterException(String message, boolean retryable, Long retryAfterMillis, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
        this.retryAfterMillis = retryAfterMillis;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
