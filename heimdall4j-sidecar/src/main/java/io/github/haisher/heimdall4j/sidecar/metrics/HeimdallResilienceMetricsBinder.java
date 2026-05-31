package io.github.haisher.heimdall4j.sidecar.metrics;

import io.github.haisher.heimdall4j.ratelimiter.RateLimiterEvent;
import io.github.haisher.heimdall4j.resilience.HeimdallPolicy;
import io.github.haisher.heimdall4j.retry.RetryEvent;
import io.github.haisher.heimdall4j.spring.HeimdallPolicyRegistry;
import io.github.haisher.heimdall4j.timeout.TimeoutEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * Subscribes to resilience policy events and records metrics via Micrometer.
 *
 * <p>Metrics exposed:</p>
 * <ul>
 *   <li>{@code heimdall4j.retry.attempts} — Counter tagged by name</li>
 *   <li>{@code heimdall4j.retry.exhausted} — Counter tagged by name</li>
 *   <li>{@code heimdall4j.retry.duration} — Timer tagged by name</li>
 *   <li>{@code heimdall4j.ratelimiter.permitted} — Counter tagged by name</li>
 *   <li>{@code heimdall4j.ratelimiter.rejected} — Counter tagged by name</li>
 *   <li>{@code heimdall4j.timeout.success} — Counter tagged by name</li>
 *   <li>{@code heimdall4j.timeout.timed_out} — Counter tagged by name</li>
 * </ul>
 */
public class HeimdallResilienceMetricsBinder {

    private final MeterRegistry meterRegistry;

    public HeimdallResilienceMetricsBinder(MeterRegistry meterRegistry, HeimdallPolicyRegistry policyRegistry) {
        this.meterRegistry = meterRegistry;
        policyRegistry.getAll().forEach(this::bind);
    }

    private void bind(HeimdallPolicy policy) {
        var name = policy.name();

        if (policy.retryExecutor() != null) {
            policy.retryExecutor().eventPublisher().subscribe(new RetryMetricsSubscriber(name));
        }

        if (policy.rateLimiterExecutor() != null) {
            policy.rateLimiterExecutor().eventPublisher().subscribe(new RateLimiterMetricsSubscriber(name));
        }

        if (policy.timeoutExecutor() != null) {
            policy.timeoutExecutor().eventPublisher().subscribe(new TimeoutMetricsSubscriber(name));
        }
    }

    private class RetryMetricsSubscriber implements Flow.Subscriber<RetryEvent> {

        private final Counter attemptCounter;
        private final Counter exhaustedCounter;
        private final Timer retryDuration;
        private Flow.Subscription subscription;

        RetryMetricsSubscriber(String name) {
            this.attemptCounter = Counter.builder("heimdall4j.retry.attempts")
                    .tag("name", name)
                    .register(meterRegistry);
            this.exhaustedCounter = Counter.builder("heimdall4j.retry.exhausted")
                    .tag("name", name)
                    .register(meterRegistry);
            this.retryDuration = Timer.builder("heimdall4j.retry.duration")
                    .tag("name", name)
                    .register(meterRegistry);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(RetryEvent event) {
            switch (event) {
                case RetryEvent.Attempt _ -> attemptCounter.increment();
                case RetryEvent.Success e -> retryDuration.record(e.totalDuration().toNanos(), TimeUnit.NANOSECONDS);
                case RetryEvent.Exhausted _ -> exhaustedCounter.increment();
            }
        }

        @Override public void onError(Throwable throwable) {}
        @Override public void onComplete() {}
    }

    private class RateLimiterMetricsSubscriber implements Flow.Subscriber<RateLimiterEvent> {

        private final Counter permittedCounter;
        private final Counter rejectedCounter;
        private Flow.Subscription subscription;

        RateLimiterMetricsSubscriber(String name) {
            this.permittedCounter = Counter.builder("heimdall4j.ratelimiter.permitted")
                    .tag("name", name)
                    .register(meterRegistry);
            this.rejectedCounter = Counter.builder("heimdall4j.ratelimiter.rejected")
                    .tag("name", name)
                    .register(meterRegistry);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(RateLimiterEvent event) {
            switch (event) {
                case RateLimiterEvent.Permitted _ -> permittedCounter.increment();
                case RateLimiterEvent.Rejected _ -> rejectedCounter.increment();
            }
        }

        @Override public void onError(Throwable throwable) {}
        @Override public void onComplete() {}
    }

    private class TimeoutMetricsSubscriber implements Flow.Subscriber<TimeoutEvent> {

        private final Counter successCounter;
        private final Counter timedOutCounter;
        private Flow.Subscription subscription;

        TimeoutMetricsSubscriber(String name) {
            this.successCounter = Counter.builder("heimdall4j.timeout.success")
                    .tag("name", name)
                    .register(meterRegistry);
            this.timedOutCounter = Counter.builder("heimdall4j.timeout.timed_out")
                    .tag("name", name)
                    .register(meterRegistry);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(TimeoutEvent event) {
            switch (event) {
                case TimeoutEvent.Success _ -> successCounter.increment();
                case TimeoutEvent.TimedOut _ -> timedOutCounter.increment();
            }
        }

        @Override public void onError(Throwable throwable) {}
        @Override public void onComplete() {}
    }
}
