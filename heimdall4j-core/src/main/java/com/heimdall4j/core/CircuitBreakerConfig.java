package com.heimdall4j.core;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Immutable configuration for a {@link CircuitBreaker}.
 * Use {@link #builder()} to construct.
 */
public record CircuitBreakerConfig(
        int failureRateThreshold,
        int ringBufferSize,
        Duration waitDurationInOpenState,
        int permittedCallsInHalfOpen,
        Duration callTimeout,
        Predicate<Throwable> recordFailure,
        Clock clock
) {

    public CircuitBreakerConfig {
        if (failureRateThreshold < 1 || failureRateThreshold > 100) {
            throw new IllegalArgumentException("failureRateThreshold must be between 1 and 100, got: " + failureRateThreshold);
        }

        if (ringBufferSize < 1) {
            throw new IllegalArgumentException("ringBufferSize must be positive, got: " + ringBufferSize);
        }

        Objects.requireNonNull(waitDurationInOpenState, "waitDurationInOpenState must not be null");

        if (waitDurationInOpenState.isNegative() || waitDurationInOpenState.isZero()) {
            throw new IllegalArgumentException("waitDurationInOpenState must be positive");
        }

        if (permittedCallsInHalfOpen < 1) {
            throw new IllegalArgumentException("permittedCallsInHalfOpen must be positive, got: " + permittedCallsInHalfOpen);
        }

        Objects.requireNonNull(callTimeout, "callTimeout must not be null");

        if (callTimeout.isNegative() || callTimeout.isZero()) {
            throw new IllegalArgumentException("callTimeout must be positive");
        }

        Objects.requireNonNull(recordFailure, "recordFailure predicate must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default configuration with sensible defaults for most use cases.
     * Equivalent to {@code CircuitBreakerConfig.builder().build()}.
     */
    public static CircuitBreakerConfig ofDefaults() {
        return new Builder().build();
    }

    public static final class Builder {
        private int failureRateThreshold = 50;
        private int ringBufferSize = 100;
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        private int permittedCallsInHalfOpen = 10;
        private Duration callTimeout = Duration.ofSeconds(5);
        private Predicate<Throwable> recordFailure = _ -> true;
        private Clock clock = Clock.systemUTC();

        Builder() {}

        public Builder failureRateThreshold(int threshold) {
            this.failureRateThreshold = threshold;
            return this;
        }

        public Builder ringBufferSize(int size) {
            this.ringBufferSize = size;
            return this;
        }

        public Builder waitDurationInOpenState(Duration duration) {
            this.waitDurationInOpenState = duration;
            return this;
        }

        public Builder permittedCallsInHalfOpen(int count) {
            this.permittedCallsInHalfOpen = count;
            return this;
        }

        public Builder callTimeout(Duration timeout) {
            this.callTimeout = timeout;
            return this;
        }

        public Builder recordFailure(Predicate<Throwable> predicate) {
            this.recordFailure = predicate;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(
                    failureRateThreshold,
                    ringBufferSize,
                    waitDurationInOpenState,
                    permittedCallsInHalfOpen,
                    callTimeout,
                    recordFailure,
                    clock
            );
        }
    }
}
