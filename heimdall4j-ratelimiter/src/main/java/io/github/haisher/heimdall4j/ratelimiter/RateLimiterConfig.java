package io.github.haisher.heimdall4j.ratelimiter;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration for a {@link RateLimiterExecutor}.
 * Uses a fixed-window algorithm: allows {@code limitForPeriod} calls per {@code refreshPeriod}.
 * Use {@link #builder()} to construct.
 */
public record RateLimiterConfig(
        int limitForPeriod,
        Duration refreshPeriod
) {

    public RateLimiterConfig {
        if (limitForPeriod < 1) {
            throw new IllegalArgumentException("limitForPeriod must be at least 1, got: " + limitForPeriod);
        }
        Objects.requireNonNull(refreshPeriod, "refreshPeriod must not be null");
        if (refreshPeriod.isNegative() || refreshPeriod.isZero()) {
            throw new IllegalArgumentException("refreshPeriod must be positive");
        }
    }

    /**
     * Creates a rate limiter config with the given limit and period.
     */
    public static RateLimiterConfig of(int limitForPeriod, Duration refreshPeriod) {
        return new RateLimiterConfig(limitForPeriod, refreshPeriod);
    }

    /** Returns a new builder with sensible defaults. */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int limitForPeriod = 50;
        private Duration refreshPeriod = Duration.ofSeconds(1);

        Builder() {}

        /** Maximum calls allowed per refresh period. Default: 50. */
        public Builder limitForPeriod(int limit) {
            this.limitForPeriod = limit;
            return this;
        }

        /** Duration of the fixed time window. Default: 1s. */
        public Builder refreshPeriod(Duration period) {
            this.refreshPeriod = period;
            return this;
        }

        public RateLimiterConfig build() {
            return new RateLimiterConfig(limitForPeriod, refreshPeriod);
        }
    }
}
