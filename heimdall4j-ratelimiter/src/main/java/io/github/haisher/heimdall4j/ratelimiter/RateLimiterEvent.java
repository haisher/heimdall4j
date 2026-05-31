package io.github.haisher.heimdall4j.ratelimiter;

/**
 * Events emitted by the {@link RateLimiterExecutor} during execution.
 * Subscribe via {@link RateLimiterExecutor#eventPublisher()}.
 */
public sealed interface RateLimiterEvent {

    String name();

    /**
     * Emitted when a call is permitted by the rate limiter.
     */
    record Permitted(String name, int remainingPermits) implements RateLimiterEvent {}

    /**
     * Emitted when a call is rejected because the rate limit is exceeded.
     */
    record Rejected(String name, int limitForPeriod) implements RateLimiterEvent {}
}
