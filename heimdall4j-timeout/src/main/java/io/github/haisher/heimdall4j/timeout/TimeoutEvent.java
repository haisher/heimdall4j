package io.github.haisher.heimdall4j.timeout;

import java.time.Duration;

/**
 * Events emitted by the {@link TimeoutExecutor} during execution.
 * Subscribe via {@link TimeoutExecutor#eventPublisher()}.
 */
public sealed interface TimeoutEvent {

    /** The name of the timeout executor that emitted this event. */
    String name();

    /** Emitted when a call completes within the configured timeout. */
    record Success(String name, Duration elapsed) implements TimeoutEvent {}

    /** Emitted when a call exceeds the configured timeout and is cancelled. */
    record TimedOut(String name, Duration timeout) implements TimeoutEvent {}
}
