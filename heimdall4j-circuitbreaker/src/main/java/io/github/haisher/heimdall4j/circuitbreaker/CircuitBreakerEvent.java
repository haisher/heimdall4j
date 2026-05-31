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

    /** The name of the circuit breaker that emitted this event. */
    String circuitBreakerName();

    /** Emitted on state transitions (e.g. CLOSED → OPEN). */
    record StateTransition(
            String circuitBreakerName,
            StateName from,
            StateName to,
            Instant timestamp
    ) implements CircuitBreakerEvent {}

    /** Emitted when a protected call succeeds. */
    record CallSuccess(
            String circuitBreakerName,
            Duration elapsed
    ) implements CircuitBreakerEvent {}

    /** Emitted when a protected call fails with a recorded exception. */
    record CallFailure(
            String circuitBreakerName,
            Throwable cause,
            Duration elapsed
    ) implements CircuitBreakerEvent {}

    /** Emitted when a protected call exceeds the configured timeout. */
    record CallTimeout(
            String circuitBreakerName,
            Duration configuredTimeout
    ) implements CircuitBreakerEvent {}
}
