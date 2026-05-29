package io.github.haisher.heimdall4j.core;

import java.time.Instant;

/**
 * Internal sealed state representation. Package-private — consumers use {@link StateName}.
 */
sealed interface State permits State.Closed, State.Open, State.HalfOpen {

    StateName name();

    record Closed(RingBuffer window) implements State {
        @Override
        public StateName name() {
            return StateName.CLOSED;
        }
    }

    record Open(Instant openedAt) implements State {
        @Override
        public StateName name() {
            return StateName.OPEN;
        }
    }

    record HalfOpen(int probeSuccessCount, int probeFailureCount, RingBuffer window) implements State {
        @Override
        public StateName name() {
            return StateName.HALF_OPEN;
        }
    }
}
