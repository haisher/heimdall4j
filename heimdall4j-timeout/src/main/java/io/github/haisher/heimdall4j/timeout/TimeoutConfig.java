package io.github.haisher.heimdall4j.timeout;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration for a {@link TimeoutExecutor}.
 * Use {@link #ofDuration(Duration)} for quick creation or {@link #builder()} for full control.
 */
public record TimeoutConfig(
        Duration duration
) {

    public TimeoutConfig {
        Objects.requireNonNull(duration, "duration must not be null");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive");
        }
    }

    /**
     * Creates a timeout config with the given duration.
     */
    public static TimeoutConfig ofDuration(Duration duration) {
        return new TimeoutConfig(duration);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Duration duration = Duration.ofSeconds(5);

        Builder() {}

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public TimeoutConfig build() {
            return new TimeoutConfig(duration);
        }
    }
}
