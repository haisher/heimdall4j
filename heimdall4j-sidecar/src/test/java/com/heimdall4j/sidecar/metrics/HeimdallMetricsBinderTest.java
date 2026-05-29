package com.heimdall4j.sidecar.metrics;

import com.heimdall4j.core.CircuitBreaker;
import com.heimdall4j.core.CircuitBreakerConfig;
import com.heimdall4j.spring.HeimdallRegistry;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class HeimdallMetricsBinderTest {

    private MeterRegistry meterRegistry;
    private HeimdallRegistry heimdallRegistry;
    private CircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        heimdallRegistry = new HeimdallRegistry();

        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(10)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedCallsInHalfOpen(2)
                .callTimeout(Duration.ofSeconds(5))
                .build();

        breaker = CircuitBreaker.of("test-service", config);
        heimdallRegistry.register(breaker);
        new HeimdallMetricsBinder(meterRegistry, heimdallRegistry);
    }

    @Test
    @DisplayName("registers state gauges for each circuit breaker")
    void stateGauges() {
        var closedGauge = meterRegistry.get("heimdall4j.state")
                .tag("name", "test-service")
                .tag("state", "CLOSED")
                .gauge();
        assertThat(closedGauge.value()).isEqualTo(1.0);

        var openGauge = meterRegistry.get("heimdall4j.state")
                .tag("name", "test-service")
                .tag("state", "OPEN")
                .gauge();
        assertThat(openGauge.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("records success counter and timer on successful call")
    void successMetrics() throws Exception {
        breaker.execute(() -> "result");
        awaitMetrics();

        var counter = meterRegistry.get("heimdall4j.calls")
                .tag("name", "test-service")
                .tag("outcome", "success")
                .counter();
        assertThat(counter.count()).isEqualTo(1.0);

        var timer = meterRegistry.get("heimdall4j.call.duration")
                .tag("name", "test-service")
                .timer();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("records failure counter on failed call")
    void failureMetrics() throws Exception {
        try {
            breaker.execute(() -> { throw new RuntimeException("boom"); });
        } catch (RuntimeException _) {}
        awaitMetrics();

        var counter = meterRegistry.get("heimdall4j.calls")
                .tag("name", "test-service")
                .tag("outcome", "failure")
                .counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("records state transition counter when breaker trips")
    void stateTransitionMetrics() throws Exception {
        // Fill buffer with failures to trip breaker (10 failures needed to fill buffer of 10)
        for (int i = 0; i < 10; i++) {
            try {
                breaker.execute(() -> { throw new RuntimeException("fail"); });
            } catch (RuntimeException _) {}
        }
        awaitMetrics();

        var counter = meterRegistry.get("heimdall4j.state.transitions")
                .tag("name", "test-service")
                .tag("from", "CLOSED")
                .tag("to", "OPEN")
                .counter();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    private void awaitMetrics() throws Exception {
        // Events are delivered asynchronously via SubmissionPublisher
        Thread.sleep(100);
    }
}
