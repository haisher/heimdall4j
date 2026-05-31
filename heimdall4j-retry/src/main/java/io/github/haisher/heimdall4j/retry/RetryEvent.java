package io.github.haisher.heimdall4j.retry;

import java.time.Duration;

/**
 * Events emitted by the {@link RetryExecutor} during execution.
 * Subscribe via {@link RetryExecutor#eventPublisher()}.
 */
public sealed interface RetryEvent {

    /** The name of the retry executor that emitted this event.
     * @return executor name
     */
    String name();

    /** Emitted before each retry attempt (not emitted for the first call).
     * @param name executor name
     * @param attemptNumber current attempt number
     * @param maxAttempts configured max attempts
     * @param delay delay before this attempt
     * @param lastException the exception that triggered this retry
     */
    record Attempt(String name, int attemptNumber, int maxAttempts, Duration delay, Throwable lastException) implements RetryEvent {}

    /** Emitted when the call succeeds (possibly after retries).
     * @param name executor name
     * @param totalAttempts total attempts made
     * @param totalDuration time from first attempt to success
     */
    record Success(String name, int totalAttempts, Duration totalDuration) implements RetryEvent {}

    /** Emitted when all retry attempts are exhausted.
     * @param name executor name
     * @param totalAttempts total attempts made
     * @param lastException the final exception
     */
    record Exhausted(String name, int totalAttempts, Throwable lastException) implements RetryEvent {}
}
