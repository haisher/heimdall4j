package io.github.haisher.heimdall4j.retry;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Immutable configuration for a {@link RetryExecutor}.
 * Use factory methods {@link #fixedDelay(int, Duration)} or {@link #exponentialBackoff(int, Duration, double)}
 * to construct, or use {@link #builder()} for full control.
 *
 * @param maxAttempts total attempts including the initial call
 * @param delay base delay between attempts
 * @param multiplier backoff multiplier applied to delay on each retry (1.0 = fixed)
 * @param retryOn predicate to determine which exceptions trigger a retry
 */
public record RetryConfig(
        int maxAttempts,
        Duration delay,
        double multiplier,
        Predicate<Throwable> retryOn
) {

    /** Validates configuration invariants. */
    public RetryConfig {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1, got: " + maxAttempts);
        }
        Objects.requireNonNull(delay, "delay must not be null");
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative");
        }
        if (multiplier < 1.0) {
            throw new IllegalArgumentException("multiplier must be >= 1.0, got: " + multiplier);
        }
        Objects.requireNonNull(retryOn, "retryOn predicate must not be null");
    }

    /**
     * Creates a retry config with fixed delay between attempts.
     *
     * @param maxAttempts total attempts including the initial call
     * @param delay fixed delay between attempts
     * @return new config instance
     */
    public static RetryConfig fixedDelay(int maxAttempts, Duration delay) {
        return new RetryConfig(maxAttempts, delay, 1.0, _ -> true);
    }

    /**
     * Creates a retry config with exponential backoff.
     * Each subsequent delay is multiplied by the given multiplier.
     *
     * @param maxAttempts total attempts including the initial call
     * @param initialDelay delay before the first retry
     * @param multiplier backoff multiplier (must be &gt;= 1.0)
     * @return new config instance
     */
    public static RetryConfig exponentialBackoff(int maxAttempts, Duration initialDelay, double multiplier) {
        return new RetryConfig(maxAttempts, initialDelay, multiplier, _ -> true);
    }

    /** Returns a new builder with sensible defaults.
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if this config uses exponential backoff (multiplier &gt; 1.0).
     *
     * @return true if exponential backoff is configured
     */
    public boolean isExponential() {
        return multiplier > 1.0;
    }

    /**
     * Calculates the delay for a given attempt (0-indexed).
     *
     * @param attempt zero-based attempt index
     * @return calculated delay for that attempt
     */
    public Duration delayForAttempt(int attempt) {
        if (attempt <= 0) return delay;
        long millis = (long) (delay.toMillis() * Math.pow(multiplier, attempt));
        return Duration.ofMillis(millis);
    }

    /** Builder for {@link RetryConfig}. */
    public static final class Builder {
        private int maxAttempts = 3;
        private Duration delay = Duration.ofMillis(500);
        private double multiplier = 1.0;
        private Predicate<Throwable> retryOn = _ -> true;

        Builder() {}

        /** Total number of attempts including the initial call. Default: 3.
         * @param maxAttempts total attempts
         * @return this builder
         */
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /** Base delay between attempts. Default: 500ms.
         * @param delay base delay duration
         * @return this builder
         */
        public Builder delay(Duration delay) {
            this.delay = delay;
            return this;
        }

        /** Backoff multiplier applied to delay on each retry. 1.0 = fixed delay. Default: 1.0.
         * @param multiplier backoff multiplier
         * @return this builder
         */
        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        /** Predicate to determine which exceptions trigger a retry. Default: all.
         * @param predicate exception predicate
         * @return this builder
         */
        public Builder retryOn(Predicate<Throwable> predicate) {
            this.retryOn = predicate;
            return this;
        }

        /** Builds an immutable {@link RetryConfig}.
         * @return new config instance
         */
        public RetryConfig build() {
            return new RetryConfig(maxAttempts, delay, multiplier, retryOn);
        }
    }
}
