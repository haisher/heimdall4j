package io.github.haisher.heimdall4j.timeout;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration for a {@link TimeoutExecutor}.
 * Use {@link #ofDuration(Duration)} for quick creation or {@link #builder()} for full control.
 *
 * @param duration maximum allowed execution time per call
 */
public record TimeoutConfig(
        Duration duration
) {

    /** Validates configuration invariants. */
    public TimeoutConfig {
        Objects.requireNonNull(duration, "duration must not be null");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive");
        }
    }

    /**
     * Creates a timeout config with the given duration.
     *
     * @param duration max execution time
     * @return new config instance
     */
    public static TimeoutConfig ofDuration(Duration duration) {
        return new TimeoutConfig(duration);
    }

    /** Returns a new builder with sensible defaults.
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link TimeoutConfig}. */
    public static final class Builder {
        private Duration duration = Duration.ofSeconds(5);

        Builder() {}

        /** Maximum allowed execution time per call. Default: 5s.
         * @param duration timeout duration
         * @return this builder
         */
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        /** Builds an immutable {@link TimeoutConfig}.
         * @return new config instance
         */
        public TimeoutConfig build() {
            return new TimeoutConfig(duration);
        }
    }
}
