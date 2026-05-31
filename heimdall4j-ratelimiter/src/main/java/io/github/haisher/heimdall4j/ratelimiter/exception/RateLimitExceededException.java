package io.github.haisher.heimdall4j.ratelimiter.exception;

/**
 * Thrown when a call is rejected because the rate limit has been exceeded.
 */
public final class RateLimitExceededException extends RuntimeException {

    /** Rate limiter name. */
    private final String name;
    /** Configured limit per period. */
    private final int limitForPeriod;

    /**
     * Creates a new rate limit exception.
     *
     * @param name rate limiter name
     * @param limitForPeriod the configured limit that was exceeded
     */
    public RateLimitExceededException(String name, int limitForPeriod) {
        super("Rate limit '%s' exceeded: %d calls per period".formatted(name, limitForPeriod));
        this.name = name;
        this.limitForPeriod = limitForPeriod;
    }

    /** Returns the name of the rate limiter that rejected the call.
     * @return rate limiter name
     */
    public String rateLimiterName() {
        return name;
    }

    /** Returns the configured limit that was exceeded.
     * @return limit per period
     */
    public int limitForPeriod() {
        return limitForPeriod;
    }
}
