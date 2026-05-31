package io.github.haisher.heimdall4j.sidecar.metrics;

import io.github.haisher.heimdall4j.ratelimiter.RateLimiterConfig;
import io.github.haisher.heimdall4j.resilience.HeimdallPolicy;
import io.github.haisher.heimdall4j.retry.RetryConfig;
import io.github.haisher.heimdall4j.retry.exception.MaxRetriesExceededException;
import io.github.haisher.heimdall4j.spring.HeimdallPolicyRegistry;
import io.github.haisher.heimdall4j.timeout.TimeoutConfig;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeimdallResilienceMetricsBinderTest {

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void recordsRetryMetrics() throws InterruptedException {
        var policyRegistry = new HeimdallPolicyRegistry();
        var policy = HeimdallPolicy.of("payments")
                .withRetry(RetryConfig.fixedDelay(3, Duration.ZERO))
                .build();
        policyRegistry.register(policy);

        new HeimdallResilienceMetricsBinder(meterRegistry, policyRegistry);
        Thread.sleep(50); // let subscribers register

        var counter = new AtomicInteger(0);
        policy.execute(() -> {
            if (counter.incrementAndGet() < 3) throw new RuntimeException("fail");
            return "ok";
        });
        Thread.sleep(50);

        var attempts = meterRegistry.find("heimdall4j.retry.attempts").counter();
        assertThat(attempts).isNotNull();
        assertThat(attempts.count()).isEqualTo(2.0); // 2 retries before success

        var duration = meterRegistry.find("heimdall4j.retry.duration").timer();
        assertThat(duration).isNotNull();
        assertThat(duration.count()).isEqualTo(1);
    }

    @Test
    void recordsRetryExhaustedMetric() throws InterruptedException {
        var policyRegistry = new HeimdallPolicyRegistry();
        var policy = HeimdallPolicy.of("test")
                .withRetry(RetryConfig.fixedDelay(2, Duration.ZERO))
                .build();
        policyRegistry.register(policy);

        new HeimdallResilienceMetricsBinder(meterRegistry, policyRegistry);
        Thread.sleep(50);

        assertThrows(MaxRetriesExceededException.class, () ->
                policy.execute(() -> { throw new RuntimeException("always"); }));
        Thread.sleep(50);

        var exhausted = meterRegistry.find("heimdall4j.retry.exhausted").counter();
        assertThat(exhausted).isNotNull();
        assertThat(exhausted.count()).isEqualTo(1.0);
    }

    @Test
    void recordsRateLimiterMetrics() throws InterruptedException {
        var policyRegistry = new HeimdallPolicyRegistry();
        var policy = HeimdallPolicy.of("api")
                .withRateLimiter(RateLimiterConfig.of(2, Duration.ofSeconds(10)))
                .build();
        policyRegistry.register(policy);

        new HeimdallResilienceMetricsBinder(meterRegistry, policyRegistry);
        Thread.sleep(50);

        policy.execute(() -> "a");
        policy.execute(() -> "b");
        try { policy.execute(() -> "c"); } catch (Exception _) {}
        Thread.sleep(50);

        var permitted = meterRegistry.find("heimdall4j.ratelimiter.permitted").counter();
        assertThat(permitted).isNotNull();
        assertThat(permitted.count()).isEqualTo(2.0);

        var rejected = meterRegistry.find("heimdall4j.ratelimiter.rejected").counter();
        assertThat(rejected).isNotNull();
        assertThat(rejected.count()).isEqualTo(1.0);
    }

    @Test
    void recordsTimeoutMetrics() throws InterruptedException {
        var policyRegistry = new HeimdallPolicyRegistry();
        var policy = HeimdallPolicy.of("slow")
                .withTimeout(TimeoutConfig.ofDuration(Duration.ofSeconds(1)))
                .build();
        policyRegistry.register(policy);

        new HeimdallResilienceMetricsBinder(meterRegistry, policyRegistry);
        Thread.sleep(50);

        policy.execute(() -> "fast");
        Thread.sleep(50);

        var success = meterRegistry.find("heimdall4j.timeout.success").counter();
        assertThat(success).isNotNull();
        assertThat(success.count()).isEqualTo(1.0);
    }
}
