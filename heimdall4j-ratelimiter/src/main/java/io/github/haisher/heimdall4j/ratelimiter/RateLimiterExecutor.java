package io.github.haisher.heimdall4j.ratelimiter;

import io.github.haisher.heimdall4j.ratelimiter.exception.RateLimitExceededException;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A fixed-window rate limiter that restricts the number of calls within a time period.
 * Thread-safe — uses lock-free AtomicReference CAS for state transitions.
 */
public final class RateLimiterExecutor {

    private final String name;
    private final RateLimiterConfig config;
    private final Clock clock;
    private final AtomicReference<Window> windowRef;
    private final SubmissionPublisher<RateLimiterEvent> eventPublisher;

    private record Window(Instant windowStart, int count) {}

    private RateLimiterExecutor(String name, RateLimiterConfig config, Clock clock) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.windowRef = new AtomicReference<>(new Window(Instant.now(clock), 0));
        this.eventPublisher = new SubmissionPublisher<>();
    }

    /**
     * Creates a new rate limiter with the given name and configuration.
     */
    public static RateLimiterExecutor of(String name, RateLimiterConfig config) {
        return new RateLimiterExecutor(name, config, Clock.systemUTC());
    }

    /**
     * Creates a new rate limiter with a custom clock (useful for testing).
     */
    public static RateLimiterExecutor of(String name, RateLimiterConfig config, Clock clock) {
        return new RateLimiterExecutor(name, config, clock);
    }

    /**
     * Executes the supplier if within the rate limit.
     *
     * @throws RateLimitExceededException if the rate limit is exceeded
     */
    public <T> T execute(Supplier<T> supplier) {
        return execute(supplier, null);
    }

    /**
     * Executes the supplier if within the rate limit.
     * If the limit is exceeded and a fallback is provided, the fallback is invoked.
     */
    public <T> T execute(Supplier<T> supplier, Supplier<T> fallback) {
        Objects.requireNonNull(supplier, "supplier must not be null");

        if (tryAcquire()) {
            return supplier.get();
        }

        emit(new RateLimiterEvent.Rejected(name, config.limitForPeriod()));

        if (fallback != null) {
            return fallback.get();
        }

        throw new RateLimitExceededException(name, config.limitForPeriod());
    }

    /**
     * Attempts to acquire a permit. Returns true if successful, false if rate limit exceeded.
     */
    public boolean tryAcquire() {
        while (true) {
            Window current = windowRef.get();
            Instant now = Instant.now(clock);

            if (now.isAfter(current.windowStart().plus(config.refreshPeriod()))) {
                var newWindow = new Window(now, 1);
                if (windowRef.compareAndSet(current, newWindow)) {
                    emit(new RateLimiterEvent.Permitted(name, config.limitForPeriod() - 1));
                    return true;
                }
                continue;
            }

            if (current.count() < config.limitForPeriod()) {
                var newWindow = new Window(current.windowStart(), current.count() + 1);
                if (windowRef.compareAndSet(current, newWindow)) {
                    emit(new RateLimiterEvent.Permitted(name, config.limitForPeriod() - newWindow.count()));
                    return true;
                }
                continue;
            }

            return false;
        }
    }

    /**
     * Returns the name of this rate limiter.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the configuration of this rate limiter.
     */
    public RateLimiterConfig config() {
        return config;
    }

    /**
     * Returns a Flow.Publisher that emits rate limiter events.
     */
    public Flow.Publisher<RateLimiterEvent> eventPublisher() {
        return eventPublisher;
    }

    /**
     * Closes the event publisher.
     */
    public void close() {
        eventPublisher.close();
    }

    private void emit(RateLimiterEvent event) {
        if (!eventPublisher.isClosed() && eventPublisher.hasSubscribers()) {
            eventPublisher.submit(event);
        }
    }
}
