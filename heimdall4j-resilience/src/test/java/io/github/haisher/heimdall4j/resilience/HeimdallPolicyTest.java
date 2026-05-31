package io.github.haisher.heimdall4j.resilience;

import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreakerConfig;
import io.github.haisher.heimdall4j.circuitbreaker.StateName;
import io.github.haisher.heimdall4j.circuitbreaker.exception.CircuitOpenException;
import io.github.haisher.heimdall4j.ratelimiter.RateLimiterConfig;
import io.github.haisher.heimdall4j.ratelimiter.exception.RateLimitExceededException;
import io.github.haisher.heimdall4j.retry.RetryConfig;
import io.github.haisher.heimdall4j.retry.exception.MaxRetriesExceededException;
import io.github.haisher.heimdall4j.timeout.TimeoutConfig;
import io.github.haisher.heimdall4j.timeout.exception.CallTimeoutException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HeimdallPolicyTest {

    @Test
    void executeWithAllLayers() {
        var policy = HeimdallPolicy.of("payments")
                .withTimeout(TimeoutConfig.ofDuration(Duration.ofSeconds(2)))
                .withRetry(RetryConfig.fixedDelay(3, Duration.ZERO))
                .withCircuitBreaker(CircuitBreakerConfig.builder().build())
                .withRateLimiter(RateLimiterConfig.of(100, Duration.ofSeconds(1)))
                .build();

        var result = policy.execute(() -> "ok");
        assertEquals("ok", result);
    }

    @Test
    void retryRecoversFromTransientFailure() {
        var counter = new AtomicInteger(0);
        var policy = HeimdallPolicy.of("test")
                .withRetry(RetryConfig.fixedDelay(3, Duration.ZERO))
                .build();

        var result = policy.execute(() -> {
            if (counter.incrementAndGet() < 3) throw new RuntimeException("transient");
            return "recovered";
        });

        assertEquals("recovered", result);
        assertEquals(3, counter.get());
    }

    @Test
    void circuitBreakerOpensAfterFailures() {
        var policy = HeimdallPolicy.of("test")
                .withCircuitBreaker(CircuitBreakerConfig.builder()
                        .ringBufferSize(2)
                        .failureRateThreshold(50)
                        .callTimeout(Duration.ofSeconds(5))
                        .build())
                .build();

        // 1 success + 1 failure fills the ring buffer at 50% failure rate
        policy.execute(() -> "ok");
        try { policy.execute(() -> { throw new RuntimeException("fail"); }); } catch (RuntimeException _) {}

        assertEquals(StateName.OPEN, policy.circuitBreaker().state());
        assertThrows(CircuitOpenException.class, () -> policy.execute(() -> "blocked"));
    }

    @Test
    void rateLimiterRejectsBeyondLimit() {
        var policy = HeimdallPolicy.of("test")
                .withRateLimiter(RateLimiterConfig.of(2, Duration.ofSeconds(10)))
                .build();

        policy.execute(() -> "a");
        policy.execute(() -> "b");
        assertThrows(RateLimitExceededException.class, () -> policy.execute(() -> "c"));
    }

    @Test
    void timeoutThrowsOnSlowCall() {
        var policy = HeimdallPolicy.of("test")
                .withTimeout(TimeoutConfig.ofDuration(Duration.ofMillis(50)))
                .build();

        assertThrows(CallTimeoutException.class, () ->
                policy.execute(() -> {
                    try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return "late";
                }));
    }

    @Test
    void retryWithCircuitBreaker() {
        var counter = new AtomicInteger(0);
        var policy = HeimdallPolicy.of("test")
                .withRetry(RetryConfig.fixedDelay(5, Duration.ZERO))
                .withCircuitBreaker(CircuitBreakerConfig.builder()
                        .ringBufferSize(10)
                        .failureRateThreshold(80)
                        .callTimeout(Duration.ofSeconds(5))
                        .build())
                .build();

        // Fails twice then succeeds — retry should handle it
        var result = policy.execute(() -> {
            if (counter.incrementAndGet() < 3) throw new RuntimeException("transient");
            return "ok";
        });

        assertEquals("ok", result);
    }

    @Test
    void fallbackInvokedWhenCircuitOpen() {
        var policy = HeimdallPolicy.of("test")
                .withCircuitBreaker(CircuitBreakerConfig.builder()
                        .ringBufferSize(2)
                        .failureRateThreshold(50)
                        .callTimeout(Duration.ofSeconds(5))
                        .build())
                .build();

        policy.execute(() -> "ok");
        try { policy.execute(() -> { throw new RuntimeException("fail"); }); } catch (RuntimeException _) {}

        var result = policy.execute(() -> "blocked", () -> "fallback");
        assertEquals("fallback", result);
    }

    @Test
    void requiresAtLeastOneStrategy() {
        assertThrows(IllegalStateException.class, () -> HeimdallPolicy.of("empty").build());
    }

    @Test
    void nameAccessor() {
        var policy = HeimdallPolicy.of("payments")
                .withRetry(RetryConfig.fixedDelay(3, Duration.ZERO))
                .build();
        assertEquals("payments", policy.name());
    }

    @Test
    void accessorsReturnNullWhenNotConfigured() {
        var policy = HeimdallPolicy.of("test")
                .withRetry(RetryConfig.fixedDelay(3, Duration.ZERO))
                .build();

        assertNotNull(policy.retryExecutor());
        assertNull(policy.circuitBreaker());
        assertNull(policy.rateLimiterExecutor());
        assertNull(policy.timeoutExecutor());
    }

    @Test
    void closeDoesNotThrow() {
        var policy = HeimdallPolicy.of("test")
                .withTimeout(TimeoutConfig.ofDuration(Duration.ofSeconds(1)))
                .withRetry(RetryConfig.fixedDelay(3, Duration.ZERO))
                .withCircuitBreaker(CircuitBreakerConfig.builder().build())
                .withRateLimiter(RateLimiterConfig.of(10, Duration.ofSeconds(1)))
                .build();

        assertDoesNotThrow(policy::close);
    }

    @Test
    void timeoutWithRetryRetriesOnTimeout() {
        var counter = new AtomicInteger(0);
        var policy = HeimdallPolicy.of("test")
                .withTimeout(TimeoutConfig.ofDuration(Duration.ofMillis(50)))
                .withRetry(RetryConfig.fixedDelay(3, Duration.ZERO))
                .build();

        var result = policy.execute(() -> {
            if (counter.incrementAndGet() < 3) {
                try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(3, counter.get());
    }

    @Test
    void fullPipelineIntegration() {
        var counter = new AtomicInteger(0);
        var policy = HeimdallPolicy.of("payments")
                .withRateLimiter(RateLimiterConfig.of(10, Duration.ofSeconds(1)))
                .withTimeout(TimeoutConfig.ofDuration(Duration.ofSeconds(1)))
                .withRetry(RetryConfig.fixedDelay(3, Duration.ZERO))
                .withCircuitBreaker(CircuitBreakerConfig.builder()
                        .ringBufferSize(10)
                        .callTimeout(Duration.ofSeconds(5))
                        .build())
                .build();

        // Fail once, then succeed
        var result = policy.execute(() -> {
            if (counter.incrementAndGet() < 2) throw new RuntimeException("transient");
            return "charged";
        });

        assertEquals("charged", result);
        assertEquals(2, counter.get());
        assertEquals(StateName.CLOSED, policy.circuitBreaker().state());
    }

    @Test
    void retryExhaustedThrowsMaxRetries() {
        var policy = HeimdallPolicy.of("test")
                .withRetry(RetryConfig.fixedDelay(2, Duration.ZERO))
                .build();

        assertThrows(MaxRetriesExceededException.class, () ->
                policy.execute(() -> { throw new RuntimeException("always fails"); }));
    }
}
