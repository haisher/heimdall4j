package io.github.haisher.heimdall4j.ratelimiter;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration for a {@link RateLimiterExecutor}.
 * Uses a fixed-window algorithm: allows {@code limitForPeriod} calls per {@code refreshPeriod}.
 * Use {@link #builder()} to construct.
 *
 * @param limitForPeriod maximum calls allowed per refresh period
 * @param refreshPeriod duration of the fixed time window
 */
public record RateLimiterConfig(
        int limitForPeriod,
        Duration refreshPeriod
) {

    /** Validates configuration invariants. */
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
     *
     * @param limitForPeriod max calls per period
     * @param refreshPeriod time window duration
     * @return new config instance
     */
    public static RateLimiterConfig of(int limitForPeriod, Duration refreshPeriod) {
        return new RateLimiterConfig(limitForPeriod, refreshPeriod);
    }

    /** Returns a new builder with sensible defaults.
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link RateLimiterConfig}. */
    public static final class Builder {
        private int limitForPeriod = 50;
        private Duration refreshPeriod = Duration.ofSeconds(1);

        Builder() {}

        /** Maximum calls allowed per refresh period. Default: 50.
         * @param limit max calls per period
         * @return this builder
         */
        public Builder limitForPeriod(int limit) {
            this.limitForPeriod = limit;
            return this;
        }

        /** Duration of the fixed time window. Default: 1s.
         * @param period time window duration
         * @return this builder
         */
        public Builder refreshPeriod(Duration period) {
            this.refreshPeriod = period;
            return this;
        }

        /** Builds an immutable {@link RateLimiterConfig}.
         * @return new config instance
         */
        public RateLimiterConfig build() {
            return new RateLimiterConfig(limitForPeriod, refreshPeriod);
        }
    }
}
