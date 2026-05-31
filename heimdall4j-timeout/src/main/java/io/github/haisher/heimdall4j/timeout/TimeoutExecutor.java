package io.github.haisher.heimdall4j.timeout;

import io.github.haisher.heimdall4j.timeout.exception.CallTimeoutException;

import java.time.Clock;
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
import java.util.function.Supplier;

/**
 * Executes a supplier with a timeout using virtual threads.
 * Thread-safe and reusable across multiple calls.
 */
public final class TimeoutExecutor {

    private final String name;
    private final TimeoutConfig config;
    private final Clock clock;
    private final java.util.concurrent.ExecutorService executor;
    private final SubmissionPublisher<TimeoutEvent> eventPublisher;

    private TimeoutExecutor(String name, TimeoutConfig config, Clock clock) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.eventPublisher = new SubmissionPublisher<>();
    }

    /**
     * Creates a new timeout executor with the given name and configuration.
     */
    public static TimeoutExecutor of(String name, TimeoutConfig config) {
        return new TimeoutExecutor(name, config, Clock.systemUTC());
    }

    /**
     * Creates a new timeout executor with a custom clock (useful for testing).
     */
    public static TimeoutExecutor of(String name, TimeoutConfig config, Clock clock) {
        return new TimeoutExecutor(name, config, clock);
    }

    /**
     * Executes the supplier with the configured timeout.
     *
     * @throws CallTimeoutException if the call exceeds the timeout
     */
    public <T> T execute(Supplier<T> supplier) {
        return execute(supplier, null);
    }

    /**
     * Executes the supplier with the configured timeout.
     * If the call times out and a fallback is provided, the fallback is invoked.
     */
    public <T> T execute(Supplier<T> supplier, Supplier<T> fallback) {
        Objects.requireNonNull(supplier, "supplier must not be null");

        Instant start = Instant.now(clock);
        var future = executor.submit((Callable<T>) supplier::get);

        try {
            T result = future.get(config.duration().toMillis(), TimeUnit.MILLISECONDS);
            emit(new TimeoutEvent.Success(name, Duration.between(start, Instant.now(clock))));
            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            emit(new TimeoutEvent.TimedOut(name, config.duration()));

            if (fallback != null) {
                return fallback.get();
            }

            throw new CallTimeoutException(name, config.duration());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Timeout executor interrupted", e);
        }
    }

    /**
     * Returns the name of this timeout executor.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the configuration of this timeout executor.
     */
    public TimeoutConfig config() {
        return config;
    }

    /**
     * Returns a Flow.Publisher that emits timeout events.
     */
    public Flow.Publisher<TimeoutEvent> eventPublisher() {
        return eventPublisher;
    }

    /**
     * Closes the event publisher.
     */
    public void close() {
        eventPublisher.close();
    }

    private void emit(TimeoutEvent event) {
        if (!eventPublisher.isClosed() && eventPublisher.hasSubscribers()) {
            eventPublisher.submit(event);
        }
    }
}
