package io.github.haisher.heimdall4j.ratelimiter.exception;

/**
 * Thrown when a call is rejected because the rate limit has been exceeded.
 */
public final class RateLimitExceededException extends RuntimeException {

    private final String name;
    private final int limitForPeriod;

    public RateLimitExceededException(String name, int limitForPeriod) {
        super("Rate limit '%s' exceeded: %d calls per period".formatted(name, limitForPeriod));
        this.name = name;
        this.limitForPeriod = limitForPeriod;
    }

    /** Returns the name of the rate limiter that rejected the call. */
    public String rateLimiterName() {
        return name;
    }

    /** Returns the configured limit that was exceeded. */
    public int limitForPeriod() {
        return limitForPeriod;
    }
}
