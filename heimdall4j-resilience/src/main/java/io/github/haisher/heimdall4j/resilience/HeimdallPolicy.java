package io.github.haisher.heimdall4j.resilience;

import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreaker;
import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreakerConfig;
import io.github.haisher.heimdall4j.ratelimiter.RateLimiterConfig;
import io.github.haisher.heimdall4j.ratelimiter.RateLimiterExecutor;
import io.github.haisher.heimdall4j.retry.RetryConfig;
import io.github.haisher.heimdall4j.retry.RetryExecutor;
import io.github.haisher.heimdall4j.timeout.TimeoutConfig;
import io.github.haisher.heimdall4j.timeout.TimeoutExecutor;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A composable resilience policy that chains multiple strategies together.
 * Execution order: RateLimiter → Retry → Timeout → CircuitBreaker → call.
 *
 * <pre>
 * var policy = HeimdallPolicy.of("payments")
 *     .withTimeout(TimeoutConfig.ofDuration(Duration.ofSeconds(2)))
 *     .withRetry(RetryConfig.exponentialBackoff(3, Duration.ofMillis(100), 2.0))
 *     .withCircuitBreaker(CircuitBreakerConfig.builder().build())
 *     .withRateLimiter(RateLimiterConfig.of(100, Duration.ofSeconds(1)))
 *     .build();
 *
 * var result = policy.execute(() -&gt; client.call());
 * </pre>
 */
public final class HeimdallPolicy {

    private final String name;
    private final CircuitBreaker circuitBreaker;
    private final RetryExecutor retryExecutor;
    private final RateLimiterExecutor rateLimiterExecutor;
    private final TimeoutExecutor timeoutExecutor;

    private HeimdallPolicy(String name,
                           CircuitBreaker circuitBreaker,
                           RetryExecutor retryExecutor,
                           RateLimiterExecutor rateLimiterExecutor,
                           TimeoutExecutor timeoutExecutor) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.circuitBreaker = circuitBreaker;
        this.retryExecutor = retryExecutor;
        this.rateLimiterExecutor = rateLimiterExecutor;
        this.timeoutExecutor = timeoutExecutor;
    }

    /**
     * Starts building a policy with the given name.
     */
    public static Builder of(String name) {
        return new Builder(name);
    }

    /**
     * Executes the supplier through the configured policy layers.
     * Execution order: RateLimiter → Retry → Timeout → CircuitBreaker → call.
     */
    public <T> T execute(Supplier<T> supplier) {
        return execute(supplier, null);
    }

    /**
     * Executes the supplier through the configured policy layers with a fallback.
     * The fallback is invoked if the outermost failing layer supports it.
     */
    public <T> T execute(Supplier<T> supplier, Supplier<T> fallback) {
        Objects.requireNonNull(supplier, "supplier must not be null");

        // Build the call chain from innermost to outermost:
        // actual call → CircuitBreaker → Timeout → Retry → RateLimiter
        Supplier<T> chain = supplier;

        if (circuitBreaker != null) {
            final Supplier<T> inner = chain;
            chain = () -> circuitBreaker.execute(inner, fallback);
        }

        if (timeoutExecutor != null) {
            final Supplier<T> inner = chain;
            final Supplier<T> timeoutFallback = (circuitBreaker == null) ? fallback : null;
            chain = () -> timeoutExecutor.execute(inner, timeoutFallback);
        }

        if (retryExecutor != null) {
            final Supplier<T> inner = chain;
            final Supplier<T> retryFallback = (circuitBreaker == null && timeoutExecutor == null) ? fallback : null;
            chain = () -> retryExecutor.execute(inner, retryFallback);
        }

        if (rateLimiterExecutor != null) {
            final Supplier<T> inner = chain;
            final Supplier<T> rlFallback = (circuitBreaker == null && timeoutExecutor == null && retryExecutor == null) ? fallback : null;
            chain = () -> rateLimiterExecutor.execute(inner, rlFallback);
        }

        return chain.get();
    }

    /**
     * Returns the name of this policy.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the circuit breaker, or null if not configured.
     */
    public CircuitBreaker circuitBreaker() {
        return circuitBreaker;
    }

    /**
     * Returns the retry executor, or null if not configured.
     */
    public RetryExecutor retryExecutor() {
        return retryExecutor;
    }

    /**
     * Returns the rate limiter executor, or null if not configured.
     */
    public RateLimiterExecutor rateLimiterExecutor() {
        return rateLimiterExecutor;
    }

    /**
     * Returns the timeout executor, or null if not configured.
     */
    public TimeoutExecutor timeoutExecutor() {
        return timeoutExecutor;
    }

    /**
     * Closes all event publishers in the policy.
     */
    public void close() {
        if (circuitBreaker != null) circuitBreaker.close();
        if (retryExecutor != null) retryExecutor.close();
        if (rateLimiterExecutor != null) rateLimiterExecutor.close();
        if (timeoutExecutor != null) timeoutExecutor.close();
    }

    public static final class Builder {
        private final String name;
        private CircuitBreakerConfig circuitBreakerConfig;
        private RetryConfig retryConfig;
        private RateLimiterConfig rateLimiterConfig;
        private TimeoutConfig timeoutConfig;

        Builder(String name) {
            this.name = Objects.requireNonNull(name, "name must not be null");
        }

        /**
         * Adds a circuit breaker layer to the policy.
         */
        public Builder withCircuitBreaker(CircuitBreakerConfig config) {
            this.circuitBreakerConfig = Objects.requireNonNull(config);
            return this;
        }

        /**
         * Adds a retry layer to the policy.
         */
        public Builder withRetry(RetryConfig config) {
            this.retryConfig = Objects.requireNonNull(config);
            return this;
        }

        /**
         * Adds a rate limiter layer to the policy.
         */
        public Builder withRateLimiter(RateLimiterConfig config) {
            this.rateLimiterConfig = Objects.requireNonNull(config);
            return this;
        }

        /**
         * Adds a timeout layer to the policy.
         */
        public Builder withTimeout(TimeoutConfig config) {
            this.timeoutConfig = Objects.requireNonNull(config);
            return this;
        }

        /**
         * Builds the policy with the configured layers.
         * At least one layer must be configured.
         */
        public HeimdallPolicy build() {
            if (circuitBreakerConfig == null && retryConfig == null &&
                    rateLimiterConfig == null && timeoutConfig == null) {
                throw new IllegalStateException("At least one resilience strategy must be configured");
            }

            CircuitBreaker cb = circuitBreakerConfig != null
                    ? CircuitBreaker.of(name, circuitBreakerConfig)
                    : null;

            RetryExecutor retry = retryConfig != null
                    ? RetryExecutor.of(name, retryConfig)
                    : null;

            RateLimiterExecutor rateLimiter = rateLimiterConfig != null
                    ? RateLimiterExecutor.of(name, rateLimiterConfig)
                    : null;

            TimeoutExecutor timeout = timeoutConfig != null
                    ? TimeoutExecutor.of(name, timeoutConfig)
                    : null;

            return new HeimdallPolicy(name, cb, retry, rateLimiter, timeout);
        }
    }
}
