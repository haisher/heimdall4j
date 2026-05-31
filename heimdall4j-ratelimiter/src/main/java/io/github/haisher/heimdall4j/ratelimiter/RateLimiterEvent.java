package io.github.haisher.heimdall4j.ratelimiter;

/**
 * Events emitted by the {@link RateLimiterExecutor} during execution.
 * Subscribe via {@link RateLimiterExecutor#eventPublisher()}.
 */
public sealed interface RateLimiterEvent {

    /** The name of the rate limiter that emitted this event.
     * @return rate limiter name
     */
    String name();

    /** Emitted when a call is permitted within the current window.
     * @param name rate limiter name
     * @param remainingPermits permits remaining in the current window
     */
    record Permitted(String name, int remainingPermits) implements RateLimiterEvent {}

    /** Emitted when a call is rejected because the window limit is reached.
     * @param name rate limiter name
     * @param limitForPeriod the configured limit that was exceeded
     */
    record Rejected(String name, int limitForPeriod) implements RateLimiterEvent {}
}
