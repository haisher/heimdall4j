package com.heimdall4j.core;

import com.heimdall4j.core.exception.CallTimeoutException;
import com.heimdall4j.core.exception.CircuitOpenException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A circuit breaker that protects calls to external services.
 * Thread-safe — uses lock-free AtomicReference CAS for state transitions.
 */
public final class CircuitBreaker {

    private final String name;
    private final CircuitBreakerConfig config;
    private final AtomicReference<State> stateRef;

    private CircuitBreaker(String name, CircuitBreakerConfig config) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.stateRef = new AtomicReference<>(new State.Closed(new RingBuffer(config.ringBufferSize())));
    }

    /**
     * Creates a new circuit breaker with the given name and configuration.
     */
    public static CircuitBreaker of(String name, CircuitBreakerConfig config) {
        return new CircuitBreaker(name, config);
    }

    /**
     * Executes the supplier through the circuit breaker with no fallback.
     *
     * @throws CircuitOpenException if the breaker is open
     * @throws CallTimeoutException if the call exceeds the configured timeout
     */
    public <T> T execute(Supplier<T> supplier) {
        return execute(supplier, null);
    }

    /**
     * Executes the supplier through the circuit breaker.
     * If the breaker is open and a fallback is provided, the fallback is invoked.
     *
     * @param supplier the call to protect
     * @param fallback optional fallback when the breaker is open or call fails
     * @return the result of the supplier or fallback
     */
    public <T> T execute(Supplier<T> supplier, Supplier<T> fallback) {
        Objects.requireNonNull(supplier, "supplier must not be null");

        // Re-read state in a loop to handle OPEN → HALF_OPEN transition
        while (true) {
            State currentState = stateRef.get();

            switch (currentState) {
                case State.Closed closed -> {
                    return executeInClosed(supplier, closed);
                }
                case State.Open open -> {
                    Instant now = Instant.now(config.clock());
                    if (now.isAfter(open.openedAt().plus(config.waitDurationInOpenState()))) {
                        // Transition to half-open and retry loop
                        var halfOpen = new State.HalfOpen(0, 0, new RingBuffer(config.permittedCallsInHalfOpen()));
                        stateRef.compareAndSet(open, halfOpen);
                        continue;
                    }
                    if (fallback != null) {
                        return fallback.get();
                    }
                    throw new CircuitOpenException(name);
                }
                case State.HalfOpen halfOpen -> {
                    return executeInHalfOpen(supplier, halfOpen, fallback);
                }
            }
        }
    }

    /**
     * Returns the current state name of this circuit breaker.
     */
    public StateName state() {
        return stateRef.get().name();
    }

    /**
     * Returns the name of this circuit breaker.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the configuration of this circuit breaker.
     */
    public CircuitBreakerConfig config() {
        return config;
    }

    private <T> T executeInClosed(Supplier<T> supplier, State.Closed closed) {
        try {
            T result = executeWithTimeout(supplier);
            recordSuccess(closed);
            return result;
        } catch (CallTimeoutException e) {
            recordFailure(closed);
            throw e;
        } catch (Exception e) {
            if (config.recordFailure().test(e)) {
                recordFailure(closed);
            }
            throw wrapIfChecked(e);
        }
    }

    private <T> T executeInHalfOpen(Supplier<T> supplier, State.HalfOpen halfOpen, Supplier<T> fallback) {
        // Check if we've exhausted probe slots
        int totalProbes = halfOpen.probeSuccessCount() + halfOpen.probeFailureCount();

        if (totalProbes >= config.permittedCallsInHalfOpen()) {
            // Already at probe limit — treat as open
            if (fallback != null) {
                return fallback.get();
            }
            throw new CircuitOpenException(name);
        }

        try {
            T result = executeWithTimeout(supplier);
            recordHalfOpenSuccess(halfOpen);
            return result;
        } catch (CallTimeoutException e) {
            recordHalfOpenFailure(halfOpen);
            throw e;
        } catch (Exception e) {
            if (config.recordFailure().test(e)) {
                recordHalfOpenFailure(halfOpen);
            }
            throw wrapIfChecked(e);
        }
    }

    private void recordSuccess(State.Closed closed) {
        var newWindow = closed.window().record(true);
        var newState = new State.Closed(newWindow);
        stateRef.compareAndSet(closed, newState);
    }

    private void recordFailure(State.Closed closed) {
        var newWindow = closed.window().record(false);

        if (newWindow.isFull() && newWindow.failureRate() >= config.failureRateThreshold()) {
            // Trip the breaker
            var openState = new State.Open(Instant.now(config.clock()));
            stateRef.compareAndSet(closed, openState);
        } else {
            var newState = new State.Closed(newWindow);
            stateRef.compareAndSet(closed, newState);
        }
    }

    private void recordHalfOpenSuccess(State.HalfOpen halfOpen) {
        int newSuccessCount = halfOpen.probeSuccessCount() + 1;

        if (newSuccessCount >= config.permittedCallsInHalfOpen()) {
            // Enough successful probes — close the breaker
            var closedState = new State.Closed(new RingBuffer(config.ringBufferSize()));
            stateRef.compareAndSet(halfOpen, closedState);
        } else {
            var newState = new State.HalfOpen(newSuccessCount, halfOpen.probeFailureCount(), halfOpen.window().record(true));
            stateRef.compareAndSet(halfOpen, newState);
        }
    }

    private void recordHalfOpenFailure(State.HalfOpen halfOpen) {
        // Any failure in half-open trips back to open
        var openState = new State.Open(Instant.now(config.clock()));
        stateRef.compareAndSet(halfOpen, openState);
    }

    private <T> T executeWithTimeout(Supplier<T> supplier) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var future = executor.submit((Callable<T>) supplier::get);
            return future.get(config.callTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new CallTimeoutException(name, config.callTimeout());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Circuit breaker call interrupted", e);
        }
    }

    private static RuntimeException wrapIfChecked(Exception e) {
        if (e instanceof RuntimeException re) {
            return re;
        }
        return new RuntimeException(e);
    }
}
