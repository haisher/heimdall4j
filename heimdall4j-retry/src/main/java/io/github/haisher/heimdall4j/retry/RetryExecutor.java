package io.github.haisher.heimdall4j.retry;

import io.github.haisher.heimdall4j.retry.exception.MaxRetriesExceededException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;

/**
 * Executes a supplier with retry logic according to a {@link RetryConfig}.
 * Thread-safe and reusable across multiple calls.
 */
public final class RetryExecutor {

    private final String name;
    private final RetryConfig config;
    private final Clock clock;
    private final SubmissionPublisher<RetryEvent> eventPublisher;

    private RetryExecutor(String name, RetryConfig config, Clock clock) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventPublisher = new SubmissionPublisher<>();
    }

    /**
     * Creates a new retry executor with the given name and configuration.
     */
    public static RetryExecutor of(String name, RetryConfig config) {
        return new RetryExecutor(name, config, Clock.systemUTC());
    }

    /**
     * Creates a new retry executor with a custom clock (useful for testing).
     */
    public static RetryExecutor of(String name, RetryConfig config, Clock clock) {
        return new RetryExecutor(name, config, clock);
    }

    /**
     * Executes the supplier, retrying on failure according to the configuration.
     *
     * @throws MaxRetriesExceededException if all attempts are exhausted
     */
    public <T> T execute(Supplier<T> supplier) {
        return execute(supplier, null);
    }

    /**
     * Executes the supplier with retries. If all attempts fail and a fallback is provided,
     * the fallback is invoked instead of throwing.
     */
    public <T> T execute(Supplier<T> supplier, Supplier<T> fallback) {
        Objects.requireNonNull(supplier, "supplier must not be null");

        Instant start = Instant.now(clock);
        Throwable lastException = null;

        for (int attempt = 1; attempt <= config.maxAttempts(); attempt++) {
            try {
                T result = supplier.get();
                emit(new RetryEvent.Success(name, attempt, Duration.between(start, Instant.now(clock))));
                return result;
            } catch (Exception e) {
                lastException = e;

                if (!config.retryOn().test(e)) {
                    throw wrapIfChecked(e);
                }

                if (attempt < config.maxAttempts()) {
                    Duration delay = config.delayForAttempt(attempt - 1);
                    emit(new RetryEvent.Attempt(name, attempt, config.maxAttempts(), delay, e));
                    sleep(delay);
                }
            }
        }

        emit(new RetryEvent.Exhausted(name, config.maxAttempts(), lastException));

        if (fallback != null) {
            return fallback.get();
        }

        throw new MaxRetriesExceededException(name, config.maxAttempts(), lastException);
    }

    /**
     * Returns the name of this retry executor.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the configuration of this retry executor.
     */
    public RetryConfig config() {
        return config;
    }

    /**
     * Returns a Flow.Publisher that emits retry events.
     */
    public Flow.Publisher<RetryEvent> eventPublisher() {
        return eventPublisher;
    }

    /**
     * Closes the event publisher.
     */
    public void close() {
        eventPublisher.close();
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry sleep interrupted", e);
        }
    }

    private void emit(RetryEvent event) {
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
