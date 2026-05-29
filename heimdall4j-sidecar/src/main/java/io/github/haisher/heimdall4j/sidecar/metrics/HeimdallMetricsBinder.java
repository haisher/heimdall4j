package io.github.haisher.heimdall4j.sidecar.metrics;

import io.github.haisher.heimdall4j.core.CircuitBreaker;
import io.github.haisher.heimdall4j.core.CircuitBreakerEvent;
import io.github.haisher.heimdall4j.core.StateName;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * Subscribes to circuit breaker events and records metrics via Micrometer.
 *
 * <p>Metrics exposed:</p>
 * <ul>
 *   <li>{@code heimdall4j.calls} — Counter tagged by name + outcome</li>
 *   <li>{@code heimdall4j.state} — Gauge (1.0 for current state, 0.0 otherwise)</li>
 *   <li>{@code heimdall4j.state.transitions} — Counter tagged by name + from + to</li>
 *   <li>{@code heimdall4j.call.duration} — Timer tagged by name</li>
 * </ul>
 */
public class HeimdallMetricsBinder {

    private final MeterRegistry meterRegistry;

    public HeimdallMetricsBinder(MeterRegistry meterRegistry, HeimdallRegistry heimdallRegistry) {
        this.meterRegistry = meterRegistry;
        heimdallRegistry.getAll().forEach(this::bind);
    }

    private void bind(CircuitBreaker breaker) {
        var name = breaker.name();

        // State gauges — one per state, value is 1.0 when active
        for (StateName state : StateName.values()) {
            meterRegistry.gauge(
                    "heimdall4j.state",
                    io.micrometer.core.instrument.Tags.of("name", name, "state", state.name()),
                    breaker,
                    cb -> cb.state() == state ? 1.0 : 0.0
            );
        }

        // Subscribe to events for counters and timers
        breaker.eventPublisher().subscribe(new MetricsSubscriber(name));
    }

    private class MetricsSubscriber implements Flow.Subscriber<CircuitBreakerEvent> {

        private final String name;
        private final Counter successCounter;
        private final Counter failureCounter;
        private final Counter timeoutCounter;
        private final Timer callDuration;
        private Flow.Subscription subscription;

        MetricsSubscriber(String name) {
            this.name = name;
            this.successCounter = Counter.builder("heimdall4j.calls")
                    .tag("name", name).tag("outcome", "success")
                    .register(meterRegistry);
            this.failureCounter = Counter.builder("heimdall4j.calls")
                    .tag("name", name).tag("outcome", "failure")
                    .register(meterRegistry);
            this.timeoutCounter = Counter.builder("heimdall4j.calls")
                    .tag("name", name).tag("outcome", "timeout")
                    .register(meterRegistry);
            this.callDuration = Timer.builder("heimdall4j.call.duration")
                    .tag("name", name)
                    .register(meterRegistry);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(CircuitBreakerEvent event) {
            switch (event) {
                case CircuitBreakerEvent.CallSuccess e -> {
                    successCounter.increment();
                    callDuration.record(e.elapsed().toNanos(), TimeUnit.NANOSECONDS);
                }
                case CircuitBreakerEvent.CallFailure e -> {
                    failureCounter.increment();
                    callDuration.record(e.elapsed().toNanos(), TimeUnit.NANOSECONDS);
                }
                case CircuitBreakerEvent.CallTimeout _ -> timeoutCounter.increment();
                case CircuitBreakerEvent.StateTransition e -> {
                    Counter.builder("heimdall4j.state.transitions")
                            .tag("name", name)
                            .tag("from", e.from().name())
                            .tag("to", e.to().name())
                            .register(meterRegistry)
                            .increment();
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            // Event delivery failed — nothing to do
        }

        @Override
        public void onComplete() {
            // Publisher closed — nothing to do
        }
    }
}
