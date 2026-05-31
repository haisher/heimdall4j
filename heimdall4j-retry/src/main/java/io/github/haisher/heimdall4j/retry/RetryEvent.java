package io.github.haisher.heimdall4j.retry;

import java.time.Duration;

/**
 * Events emitted by the {@link RetryExecutor} during execution.
 * Subscribe via {@link RetryExecutor#eventPublisher()}.
 */
public sealed interface RetryEvent {

    String name();

    /**
     * Emitted when a retry attempt is about to be made after a failure.
     */
    record Attempt(String name, int attemptNumber, int maxAttempts, Duration delay, Throwable lastException) implements RetryEvent {}

    /**
     * Emitted when the call succeeds (possibly after retries).
     */
    record Success(String name, int totalAttempts, Duration totalDuration) implements RetryEvent {}

    /**
     * Emitted when all retry attempts are exhausted.
     */
    record Exhausted(String name, int totalAttempts, Throwable lastException) implements RetryEvent {}
}
