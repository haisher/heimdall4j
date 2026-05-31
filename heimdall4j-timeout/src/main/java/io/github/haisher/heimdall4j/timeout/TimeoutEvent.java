package io.github.haisher.heimdall4j.timeout;

import java.time.Duration;

/**
 * Events emitted by the {@link TimeoutExecutor} during execution.
 * Subscribe via {@link TimeoutExecutor#eventPublisher()}.
 */
public sealed interface TimeoutEvent {

    String name();

    /**
     * Emitted when a call completes within the timeout.
     */
    record Success(String name, Duration elapsed) implements TimeoutEvent {}

    /**
     * Emitted when a call exceeds the configured timeout.
     */
    record TimedOut(String name, Duration timeout) implements TimeoutEvent {}
}
