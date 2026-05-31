package io.github.haisher.heimdall4j.circuitbreaker;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Immutable configuration for a {@link CircuitBreaker}.
 * Use {@link #builder()} to construct.
 *
 * @param failureRateThreshold percentage (1–100) that trips the circuit
 * @param ringBufferSize number of calls tracked for failure rate calculation
 * @param waitDurationInOpenState how long the circuit stays open before half-open
 * @param permittedCallsInHalfOpen probe calls allowed in half-open
 * @param callTimeout max duration per call before timeout
 * @param recordFailure predicate to determine which exceptions count as failures
 * @param clock clock for time-based operations
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

    /** Validates configuration invariants. */
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

    /**
     * Returns a new builder with sensible defaults.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default configuration. Equivalent to {@code builder().build()}.
     *
     * @return default config instance
     */
    public static CircuitBreakerConfig ofDefaults() {
        return new Builder().build();
    }

    /** Builder for {@link CircuitBreakerConfig}. */
    public static final class Builder {
        private int failureRateThreshold = 50;
        private int ringBufferSize = 100;
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        private int permittedCallsInHalfOpen = 10;
        private Duration callTimeout = Duration.ofSeconds(5);
        private Predicate<Throwable> recordFailure = _ -> true;
        private Clock clock = Clock.systemUTC();

        Builder() {}

        /** Failure percentage (1–100) that trips the circuit. Default: 50.
         * @param threshold failure rate percentage
         * @return this builder
         */
        public Builder failureRateThreshold(int threshold) {
            this.failureRateThreshold = threshold;
            return this;
        }

        /** Number of calls tracked for failure rate calculation. Default: 100.
         * @param size ring buffer size
         * @return this builder
         */
        public Builder ringBufferSize(int size) {
            this.ringBufferSize = size;
            return this;
        }

        /** How long the circuit stays open before transitioning to half-open. Default: 30s.
         * @param duration wait duration in open state
         * @return this builder
         */
        public Builder waitDurationInOpenState(Duration duration) {
            this.waitDurationInOpenState = duration;
            return this;
        }

        /** Number of probe calls allowed in half-open before closing. Default: 10.
         * @param count permitted calls in half-open
         * @return this builder
         */
        public Builder permittedCallsInHalfOpen(int count) {
            this.permittedCallsInHalfOpen = count;
            return this;
        }

        /** Maximum duration per call before timeout. Default: 5s.
         * @param timeout call timeout duration
         * @return this builder
         */
        public Builder callTimeout(Duration timeout) {
            this.callTimeout = timeout;
            return this;
        }

        /** Predicate to determine which exceptions count as failures. Default: all.
         * @param predicate exception predicate
         * @return this builder
         */
        public Builder recordFailure(Predicate<Throwable> predicate) {
            this.recordFailure = predicate;
            return this;
        }

        /** Clock for time-based operations. Override for testing. Default: system UTC.
         * @param clock clock instance
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /** Builds an immutable {@link CircuitBreakerConfig}.
         * @return new config instance
         */
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
