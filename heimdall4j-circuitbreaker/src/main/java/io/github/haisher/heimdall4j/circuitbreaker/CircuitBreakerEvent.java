package io.github.haisher.heimdall4j.circuitbreaker;

import java.time.Duration;
import java.time.Instant;

/**
 * Events emitted by a {@link CircuitBreaker} during its lifecycle.
 * Subscribe via {@link CircuitBreaker#eventPublisher()}.
 */
public sealed interface CircuitBreakerEvent permits
        CircuitBreakerEvent.StateTransition,
        CircuitBreakerEvent.CallSuccess,
        CircuitBreakerEvent.CallFailure,
        CircuitBreakerEvent.CallTimeout {

    /** The name of the circuit breaker that emitted this event.
     * @return circuit breaker name
     */
    String circuitBreakerName();

    /** Emitted on state transitions (e.g. CLOSED → OPEN).
     * @param circuitBreakerName circuit breaker name
     * @param from previous state
     * @param to new state
     * @param timestamp time of transition
     */
    record StateTransition(
            String circuitBreakerName,
            StateName from,
            StateName to,
            Instant timestamp
    ) implements CircuitBreakerEvent {}

    /** Emitted when a protected call succeeds.
     * @param circuitBreakerName circuit breaker name
     * @param elapsed call duration
     */
    record CallSuccess(
            String circuitBreakerName,
            Duration elapsed
    ) implements CircuitBreakerEvent {}

    /** Emitted when a protected call fails with a recorded exception.
     * @param circuitBreakerName circuit breaker name
     * @param cause the exception that caused the failure
     * @param elapsed call duration
     */
    record CallFailure(
            String circuitBreakerName,
            Throwable cause,
            Duration elapsed
    ) implements CircuitBreakerEvent {}

    /** Emitted when a protected call exceeds the configured timeout.
     * @param circuitBreakerName circuit breaker name
     * @param configuredTimeout the timeout that was exceeded
     */
    record CallTimeout(
            String circuitBreakerName,
            Duration configuredTimeout
    ) implements CircuitBreakerEvent {}
}
