package io.github.haisher.heimdall4j.retry;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Immutable configuration for a {@link RetryExecutor}.
 * Use factory methods {@link #fixedDelay(int, Duration)} or {@link #exponentialBackoff(int, Duration, double)}
 * to construct, or use {@link #builder()} for full control.
 */
public record RetryConfig(
        int maxAttempts,
        Duration delay,
        double multiplier,
        Predicate<Throwable> retryOn
) {

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
     */
    public static RetryConfig fixedDelay(int maxAttempts, Duration delay) {
        return new RetryConfig(maxAttempts, delay, 1.0, _ -> true);
    }

    /**
     * Creates a retry config with exponential backoff.
     * Each subsequent delay is multiplied by the given multiplier.
     */
    public static RetryConfig exponentialBackoff(int maxAttempts, Duration initialDelay, double multiplier) {
        return new RetryConfig(maxAttempts, initialDelay, multiplier, _ -> true);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if this config uses exponential backoff (multiplier > 1.0).
     */
    public boolean isExponential() {
        return multiplier > 1.0;
    }

    /**
     * Calculates the delay for a given attempt (0-indexed).
     */
    public Duration delayForAttempt(int attempt) {
        if (attempt <= 0) return delay;
        long millis = (long) (delay.toMillis() * Math.pow(multiplier, attempt));
        return Duration.ofMillis(millis);
    }

    public static final class Builder {
        private int maxAttempts = 3;
        private Duration delay = Duration.ofMillis(500);
        private double multiplier = 1.0;
        private Predicate<Throwable> retryOn = _ -> true;

        Builder() {}

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder delay(Duration delay) {
            this.delay = delay;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder retryOn(Predicate<Throwable> predicate) {
            this.retryOn = predicate;
            return this;
        }

        public RetryConfig build() {
            return new RetryConfig(maxAttempts, delay, multiplier, retryOn);
        }
    }
}
