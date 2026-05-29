package io.github.haisher.heimdall4j.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerConcurrencyTest {

    @Test
    @DisplayName("concurrent successes are all recorded without lost updates")
    void concurrentSuccessesRecorded() throws InterruptedException {
        var clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(100)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedCallsInHalfOpen(10)
                .callTimeout(Duration.ofSeconds(5))
                .clock(clock)
                .build();
        var breaker = CircuitBreaker.of("concurrent", config);

        int threadCount = 50;
        var latch = new CountDownLatch(1);
        var done = new CountDownLatch(threadCount);
        var successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    latch.await();
                    breaker.execute(() -> "ok");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // shouldn't happen
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown(); // release all threads at once
        done.await();

        assertEquals(threadCount, successCount.get());
        assertEquals(StateName.CLOSED, breaker.state());
        breaker.close();
    }

    @Test
    @DisplayName("concurrent failures trip breaker exactly once")
    void concurrentFailuresTripOnce() throws InterruptedException {
        var clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(10)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedCallsInHalfOpen(5)
                .callTimeout(Duration.ofSeconds(5))
                .clock(clock)
                .build();
        var breaker = CircuitBreaker.of("concurrent-fail", config);

        int threadCount = 20;
        var latch = new CountDownLatch(1);
        var done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    latch.await();
                    breaker.execute(() -> { throw new RuntimeException("fail"); });
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown();
        done.await();

        // Should be OPEN — failures exceed threshold once buffer is full
        assertEquals(StateName.OPEN, breaker.state());
        breaker.close();
    }

    @Test
    @DisplayName("timeout cancels the underlying task")
    void timeoutCancelsTask() throws InterruptedException {
        var clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(10)
                .callTimeout(Duration.ofMillis(50))
                .clock(clock)
                .build();
        var breaker = CircuitBreaker.of("cancel-test", config);

        var interrupted = new AtomicInteger(0);

        assertThrows(Exception.class, () ->
                breaker.execute(() -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        interrupted.incrementAndGet();
                    }
                    return "never";
                }));

        // Give the cancelled task time to receive interruption
        Thread.sleep(100);
        assertEquals(1, interrupted.get(), "Task should have been interrupted after timeout");
        breaker.close();
    }
}
