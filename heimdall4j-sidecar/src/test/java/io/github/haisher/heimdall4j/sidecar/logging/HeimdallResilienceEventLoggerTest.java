package io.github.haisher.heimdall4j.sidecar.logging;

import io.github.haisher.heimdall4j.ratelimiter.RateLimiterConfig;
import io.github.haisher.heimdall4j.resilience.HeimdallPolicy;
import io.github.haisher.heimdall4j.retry.RetryConfig;
import io.github.haisher.heimdall4j.spring.HeimdallPolicyRegistry;
import io.github.haisher.heimdall4j.timeout.TimeoutConfig;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HeimdallResilienceEventLoggerTest {

    @Test
    void subscribesWithoutErrors() throws InterruptedException {
        var policyRegistry = new HeimdallPolicyRegistry();
        var policy = HeimdallPolicy.of("test")
                .withRetry(RetryConfig.fixedDelay(3, Duration.ZERO))
                .withRateLimiter(RateLimiterConfig.of(100, Duration.ofSeconds(1)))
                .withTimeout(TimeoutConfig.ofDuration(Duration.ofSeconds(1)))
                .build();
        policyRegistry.register(policy);

        assertDoesNotThrow(() -> new HeimdallResilienceEventLogger(policyRegistry));
        Thread.sleep(50);

        var counter = new AtomicInteger(0);
        var result = policy.execute(() -> {
            if (counter.incrementAndGet() < 2) throw new RuntimeException("transient");
            return "ok";
        });

        assertEquals("ok", result);
    }

    @Test
    void handlesEmptyRegistry() {
        var policyRegistry = new HeimdallPolicyRegistry();
        assertDoesNotThrow(() -> new HeimdallResilienceEventLogger(policyRegistry));
    }

    @Test
    void handlesPolicyWithPartialConfig() throws InterruptedException {
        var policyRegistry = new HeimdallPolicyRegistry();
        var policy = HeimdallPolicy.of("retryOnly")
                .withRetry(RetryConfig.fixedDelay(2, Duration.ZERO))
                .build();
        policyRegistry.register(policy);

        assertDoesNotThrow(() -> new HeimdallResilienceEventLogger(policyRegistry));
        Thread.sleep(50);

        var result = policy.execute(() -> "ok");
        assertEquals("ok", result);
    }
}
