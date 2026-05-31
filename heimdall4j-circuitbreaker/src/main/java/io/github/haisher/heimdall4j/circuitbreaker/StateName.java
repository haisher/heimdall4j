package io.github.haisher.heimdall4j.circuitbreaker;

/**
 * Circuit breaker state names.
 */
public enum StateName {
    /** Normal operation — calls are executed and outcomes recorded. */
    CLOSED,
    /** Circuit is tripped — calls are rejected immediately. */
    OPEN,
    /** Probing — a limited number of calls are allowed to test recovery. */
    HALF_OPEN
}
