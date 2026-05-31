package io.github.haisher.heimdall4j.ratelimiter;

import io.github.haisher.heimdall4j.ratelimiter.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterExecutorTest {

    @Test
    void permitsCallsWithinLimit() {
        var config = RateLimiterConfig.of(3, Duration.ofSeconds(1));
        var executor = RateLimiterExecutor.of("test", config);

        assertEquals("a", executor.execute(() -> "a"));
        assertEquals("b", executor.execute(() -> "b"));
        assertEquals("c", executor.execute(() -> "c"));
    }

    @Test
    void rejectsCallsOverLimit() {
        var config = RateLimiterConfig.of(2, Duration.ofSeconds(1));
        var executor = RateLimiterExecutor.of("test", config);

        executor.execute(() -> "a");
        executor.execute(() -> "b");

        assertThrows(RateLimitExceededException.class, () -> executor.execute(() -> "c"));
    }

    @Test
    void exceptionContainsDetails() {
        var config = RateLimiterConfig.of(1, Duration.ofSeconds(1));
        var executor = RateLimiterExecutor.of("payments", config);
        executor.execute(() -> "first");

        var ex = assertThrows(RateLimitExceededException.class, () -> executor.execute(() -> "second"));
        assertEquals("payments", ex.rateLimiterName());
        assertEquals(1, ex.limitForPeriod());
    }

    @Test
    void invokesFallbackWhenLimitExceeded() {
        var config = RateLimiterConfig.of(1, Duration.ofSeconds(1));
        var executor = RateLimiterExecutor.of("test", config);
        executor.execute(() -> "first");

        var result = executor.execute(() -> "second", () -> "fallback");
        assertEquals("fallback", result);
    }

    @Test
    void resetsAfterRefreshPeriod() {
        var now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        var clock = new Clock() {
            @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };

        var config = RateLimiterConfig.of(2, Duration.ofSeconds(1));
        var executor = RateLimiterExecutor.of("test", config, clock);

        executor.execute(() -> "a");
        executor.execute(() -> "b");
        assertThrows(RateLimitExceededException.class, () -> executor.execute(() -> "c"));

        // Advance past refresh period
        now.set(now.get().plusSeconds(2));

        // Should be allowed again
        assertEquals("d", executor.execute(() -> "d"));
    }

    @Test
    void tryAcquireReturnsCorrectly() {
        var config = RateLimiterConfig.of(2, Duration.ofSeconds(1));
        var executor = RateLimiterExecutor.of("test", config);

        assertTrue(executor.tryAcquire());
        assertTrue(executor.tryAcquire());
        assertFalse(executor.tryAcquire());
    }

    @Test
    void emitsPermittedAndRejectedEvents() throws InterruptedException {
        var events = new ArrayList<RateLimiterEvent>();
        var config = RateLimiterConfig.of(1, Duration.ofSeconds(1));
        var executor = RateLimiterExecutor.of("test", config);

        executor.eventPublisher().subscribe(new Flow.Subscriber<>() {
            @Override public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            @Override public void onNext(RateLimiterEvent item) { events.add(item); }
            @Override public void onError(Throwable t) {}
            @Override public void onComplete() {}
        });

        Thread.sleep(50);

        executor.execute(() -> "ok");
        try { executor.execute(() -> "rejected"); } catch (RateLimitExceededException _) {}

        Thread.sleep(50);

        assertEquals(2, events.size());
        assertInstanceOf(RateLimiterEvent.Permitted.class, events.get(0));
        assertInstanceOf(RateLimiterEvent.Rejected.class, events.get(1));
    }

    @Test
    void nameAndConfigAccessors() {
        var config = RateLimiterConfig.of(10, Duration.ofSeconds(1));
        var executor = RateLimiterExecutor.of("myLimiter", config);
        assertEquals("myLimiter", executor.name());
        assertSame(config, executor.config());
    }

    @Test
    void concurrentAccessRespectLimit() throws InterruptedException {
        var config = RateLimiterConfig.of(100, Duration.ofSeconds(10));
        var executor = RateLimiterExecutor.of("test", config);
        var counter = new java.util.concurrent.atomic.AtomicInteger(0);
        var rejected = new java.util.concurrent.atomic.AtomicInteger(0);

        var threads = new Thread[200];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = Thread.ofVirtual().start(() -> {
                try {
                    executor.execute(() -> {
                        counter.incrementAndGet();
                        return null;
                    });
                } catch (RateLimitExceededException _) {
                    rejected.incrementAndGet();
                }
            });
        }

        for (var t : threads) t.join();

        assertEquals(100, counter.get());
        assertEquals(100, rejected.get());
    }

    @Test
    void closeStopsEventPublisher() {
        var executor = RateLimiterExecutor.of("test", RateLimiterConfig.of(10, Duration.ofSeconds(1)));
        executor.close();
        assertEquals("ok", executor.execute(() -> "ok"));
    }
}
