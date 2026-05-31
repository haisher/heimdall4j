package io.github.haisher.heimdall4j.retry;

import io.github.haisher.heimdall4j.retry.exception.MaxRetriesExceededException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryExecutorTest {

    @Test
    void succeedsOnFirstAttempt() {
        var executor = RetryExecutor.of("test", RetryConfig.fixedDelay(3, Duration.ZERO));
        var result = executor.execute(() -> "ok");
        assertEquals("ok", result);
    }

    @Test
    void retriesAndSucceeds() {
        var counter = new AtomicInteger(0);
        var executor = RetryExecutor.of("test", RetryConfig.fixedDelay(3, Duration.ZERO));

        var result = executor.execute(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new RuntimeException("fail");
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(3, counter.get());
    }

    @Test
    void throwsMaxRetriesExceededWhenExhausted() {
        var executor = RetryExecutor.of("payments", RetryConfig.fixedDelay(2, Duration.ZERO));

        var ex = assertThrows(MaxRetriesExceededException.class, () ->
                executor.execute(() -> { throw new RuntimeException("boom"); }));

        assertEquals("payments", ex.retryName());
        assertEquals(2, ex.attempts());
        assertNotNull(ex.getCause());
    }

    @Test
    void invokesFallbackWhenExhausted() {
        var executor = RetryExecutor.of("test", RetryConfig.fixedDelay(2, Duration.ZERO));

        var result = executor.execute(
                () -> { throw new RuntimeException("fail"); },
                () -> "fallback"
        );

        assertEquals("fallback", result);
    }

    @Test
    void respectsRetryOnPredicate() {
        var config = RetryConfig.builder()
                .maxAttempts(3)
                .delay(Duration.ZERO)
                .retryOn(e -> e instanceof IllegalStateException)
                .build();
        var executor = RetryExecutor.of("test", config);

        // NullPointerException should NOT be retried
        assertThrows(NullPointerException.class, () ->
                executor.execute(() -> { throw new NullPointerException("nope"); }));
    }

    @Test
    void emitsEventsOnRetryAndSuccess() throws InterruptedException {
        var events = new ArrayList<RetryEvent>();
        var executor = RetryExecutor.of("test", RetryConfig.fixedDelay(3, Duration.ZERO));

        executor.eventPublisher().subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;
            @Override public void onSubscribe(Flow.Subscription s) { subscription = s; s.request(Long.MAX_VALUE); }
            @Override public void onNext(RetryEvent item) { events.add(item); }
            @Override public void onError(Throwable t) {}
            @Override public void onComplete() {}
        });

        Thread.sleep(50); // let subscriber register

        var counter = new AtomicInteger(0);
        executor.execute(() -> {
            if (counter.incrementAndGet() < 3) throw new RuntimeException("fail");
            return "ok";
        });

        Thread.sleep(50); // let events propagate

        assertEquals(3, events.size());
        assertInstanceOf(RetryEvent.Attempt.class, events.get(0));
        assertInstanceOf(RetryEvent.Attempt.class, events.get(1));
        assertInstanceOf(RetryEvent.Success.class, events.get(2));

        var success = (RetryEvent.Success) events.get(2);
        assertEquals(3, success.totalAttempts());
    }

    @Test
    void emitsExhaustedEvent() throws InterruptedException {
        var events = new ArrayList<RetryEvent>();
        var executor = RetryExecutor.of("test", RetryConfig.fixedDelay(2, Duration.ZERO));

        executor.eventPublisher().subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;
            @Override public void onSubscribe(Flow.Subscription s) { subscription = s; s.request(Long.MAX_VALUE); }
            @Override public void onNext(RetryEvent item) { events.add(item); }
            @Override public void onError(Throwable t) {}
            @Override public void onComplete() {}
        });

        Thread.sleep(50);

        assertThrows(MaxRetriesExceededException.class, () ->
                executor.execute(() -> { throw new RuntimeException("boom"); }));

        Thread.sleep(50);

        var last = events.getLast();
        assertInstanceOf(RetryEvent.Exhausted.class, last);
    }

    @Test
    void exponentialBackoffIncreasesDelay() {
        var config = RetryConfig.exponentialBackoff(4, Duration.ofMillis(10), 2.0);
        var executor = RetryExecutor.of("test", config);
        var counter = new AtomicInteger(0);

        long start = System.currentTimeMillis();
        executor.execute(() -> {
            if (counter.incrementAndGet() < 4) throw new RuntimeException("fail");
            return "ok";
        });
        long elapsed = System.currentTimeMillis() - start;

        // Expected delays: 10 + 20 + 40 = 70ms minimum
        assertTrue(elapsed >= 60, "Expected at least 60ms for exponential backoff, got: " + elapsed);
    }

    @Test
    void nameAndConfigAccessors() {
        var config = RetryConfig.fixedDelay(3, Duration.ofMillis(100));
        var executor = RetryExecutor.of("myRetry", config);
        assertEquals("myRetry", executor.name());
        assertSame(config, executor.config());
    }

    @Test
    void closeStopsEventPublisher() {
        var executor = RetryExecutor.of("test", RetryConfig.fixedDelay(3, Duration.ZERO));
        executor.close();
        // Should not throw — just silently skips emit
        var result = executor.execute(() -> "ok");
        assertEquals("ok", result);
    }
}
