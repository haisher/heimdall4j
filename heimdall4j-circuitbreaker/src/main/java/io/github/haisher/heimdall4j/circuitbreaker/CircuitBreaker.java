package io.github.haisher.heimdall4j.circuitbreaker;

import io.github.haisher.heimdall4j.circuitbreaker.exception.CallTimeoutException;
import io.github.haisher.heimdall4j.circuitbreaker.exception.CircuitOpenException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
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
    private final SubmissionPublisher<CircuitBreakerEvent> eventPublisher;
    private final java.util.concurrent.ExecutorService timeoutExecutor;

    private CircuitBreaker(String name, CircuitBreakerConfig config) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.stateRef = new AtomicReference<>(new State.Closed(new RingBuffer(config.ringBufferSize())));
        this.eventPublisher = new SubmissionPublisher<>();
        this.timeoutExecutor = Executors.newVirtualThreadPerTaskExecutor();
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
                        var halfOpen = new State.HalfOpen(0, 0, new RingBuffer(config.permittedCallsInHalfOpen()));
                        if (stateRef.compareAndSet(open, halfOpen)) {
                            emit(new CircuitBreakerEvent.StateTransition(name, StateName.OPEN, StateName.HALF_OPEN, now));
                        }
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

    /**
     * Returns a Flow.Publisher that emits circuit breaker events.
     * Subscribers receive events for state transitions, call outcomes, and timeouts.
     */
    public Flow.Publisher<CircuitBreakerEvent> eventPublisher() {
        return eventPublisher;
    }

    /**
     * Closes the event publisher, stopping event emission.
     * The circuit breaker remains functional after close — it just stops publishing events.
     */
    public void close() {
        eventPublisher.close();
    }

    private <T> T executeInClosed(Supplier<T> supplier, State.Closed closed) {
        Instant start = Instant.now(config.clock());
        try {
            T result = executeWithTimeout(supplier);
            recordSuccess(closed);
            emit(new CircuitBreakerEvent.CallSuccess(name, Duration.between(start, Instant.now(config.clock()))));
            return result;
        } catch (CallTimeoutException e) {
            recordFailure(closed);
            emit(new CircuitBreakerEvent.CallTimeout(name, config.callTimeout()));
            throw e;
        } catch (Exception e) {
            if (config.recordFailure().test(e)) {
                recordFailure(closed);
                emit(new CircuitBreakerEvent.CallFailure(name, e, Duration.between(start, Instant.now(config.clock()))));
            }
            throw wrapIfChecked(e);
        }
    }

    private <T> T executeInHalfOpen(Supplier<T> supplier, State.HalfOpen halfOpen, Supplier<T> fallback) {
        int totalProbes = halfOpen.probeSuccessCount() + halfOpen.probeFailureCount();

        if (totalProbes >= config.permittedCallsInHalfOpen()) {
            if (fallback != null) {
                return fallback.get();
            }
            throw new CircuitOpenException(name);
        }

        Instant start = Instant.now(config.clock());
        try {
            T result = executeWithTimeout(supplier);
            recordHalfOpenSuccess(halfOpen);
            emit(new CircuitBreakerEvent.CallSuccess(name, Duration.between(start, Instant.now(config.clock()))));
            return result;
        } catch (CallTimeoutException e) {
            recordHalfOpenFailure(halfOpen);
            emit(new CircuitBreakerEvent.CallTimeout(name, config.callTimeout()));
            throw e;
        } catch (Exception e) {
            if (config.recordFailure().test(e)) {
                recordHalfOpenFailure(halfOpen);
                emit(new CircuitBreakerEvent.CallFailure(name, e, Duration.between(start, Instant.now(config.clock()))));
            }
            throw wrapIfChecked(e);
        }
    }

    private void recordSuccess(State.Closed closed) {
        var current = closed;
        while (true) {
            var newWindow = current.window().record(true);
            var newState = new State.Closed(newWindow);
            if (stateRef.compareAndSet(current, newState)) return;
            State s = stateRef.get();
            if (s instanceof State.Closed c) { current = c; }
            else return;
        }
    }

    private void recordFailure(State.Closed closed) {
        var current = closed;
        while (true) {
            var newWindow = current.window().record(false);

            if (newWindow.isFull() && newWindow.failureRate() >= config.failureRateThreshold()) {
                var openState = new State.Open(Instant.now(config.clock()));
                if (stateRef.compareAndSet(current, openState)) {
                    emit(new CircuitBreakerEvent.StateTransition(name, StateName.CLOSED, StateName.OPEN, Instant.now(config.clock())));
                    return;
                }
            } else {
                var newState = new State.Closed(newWindow);
                if (stateRef.compareAndSet(current, newState)) return;
            }

            State s = stateRef.get();
            if (s instanceof State.Closed c) { current = c; }
            else return;
        }
    }

    private void recordHalfOpenSuccess(State.HalfOpen halfOpen) {
        var current = halfOpen;
        while (true) {
            int newSuccessCount = current.probeSuccessCount() + 1;

            if (newSuccessCount >= config.permittedCallsInHalfOpen()) {
                var closedState = new State.Closed(new RingBuffer(config.ringBufferSize()));
                if (stateRef.compareAndSet(current, closedState)) {
                    emit(new CircuitBreakerEvent.StateTransition(name, StateName.HALF_OPEN, StateName.CLOSED, Instant.now(config.clock())));
                    return;
                }
            } else {
                var newState = new State.HalfOpen(newSuccessCount, current.probeFailureCount(), current.window().record(true));
                if (stateRef.compareAndSet(current, newState)) return;
            }

            State s = stateRef.get();
            if (s instanceof State.HalfOpen h) { current = h; }
            else return;
        }
    }

    private void recordHalfOpenFailure(State.HalfOpen halfOpen) {
        var current = halfOpen;
        while (true) {
            var openState = new State.Open(Instant.now(config.clock()));
            if (stateRef.compareAndSet(current, openState)) {
                emit(new CircuitBreakerEvent.StateTransition(name, StateName.HALF_OPEN, StateName.OPEN, Instant.now(config.clock())));
                return;
            }
            State s = stateRef.get();
            if (s instanceof State.HalfOpen h) { current = h; }
            else return;
        }
    }

    private <T> T executeWithTimeout(Supplier<T> supplier) {
        var future = timeoutExecutor.submit((Callable<T>) supplier::get);
        try {
            return future.get(config.callTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new CallTimeoutException(name, config.callTimeout());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Circuit breaker call interrupted", e);
        }
    }

    private void emit(CircuitBreakerEvent event) {
        if (!eventPublisher.isClosed() && eventPublisher.hasSubscribers()) {
            eventPublisher.submit(event);
        }
    }

    private static RuntimeException wrapIfChecked(Exception e) {
        if (e instanceof RuntimeException re) {
            return re;
        }

        return new RuntimeException(e);
    }
}
