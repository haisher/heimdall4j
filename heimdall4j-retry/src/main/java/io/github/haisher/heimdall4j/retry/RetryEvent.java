package io.github.haisher.heimdall4j.retry;

import java.time.Duration;

/**
 * Events emitted by the {@link RetryExecutor} during execution.
 * Subscribe via {@link RetryExecutor#eventPublisher()}.
 */
public sealed interface RetryEvent {

    /** The name of the retry executor that emitted this event. */
    String name();

    /** Emitted before each retry attempt (not emitted for the first call). */
    record Attempt(String name, int attemptNumber, int maxAttempts, Duration delay, Throwable lastException) implements RetryEvent {}

    /** Emitted when the call succeeds (possibly after retries). */
    record Success(String name, int totalAttempts, Duration totalDuration) implements RetryEvent {}

    /** Emitted when all retry attempts are exhausted. */
    record Exhausted(String name, int totalAttempts, Throwable lastException) implements RetryEvent {}
}
