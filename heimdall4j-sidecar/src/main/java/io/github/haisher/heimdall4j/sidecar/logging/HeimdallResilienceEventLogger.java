package io.github.haisher.heimdall4j.sidecar.logging;

import io.github.haisher.heimdall4j.ratelimiter.RateLimiterEvent;
import io.github.haisher.heimdall4j.resilience.HeimdallPolicy;
import io.github.haisher.heimdall4j.retry.RetryEvent;
import io.github.haisher.heimdall4j.spring.HeimdallPolicyRegistry;
import io.github.haisher.heimdall4j.timeout.TimeoutEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;

/**
 * Subscribes to resilience policy events and logs them via SLF4J.
 *
 * <p>Log levels:</p>
 * <ul>
 *   <li>Retry attempt → INFO</li>
 *   <li>Retry exhausted → WARN</li>
 *   <li>Retry success → DEBUG</li>
 *   <li>Rate limiter rejected → WARN</li>
 *   <li>Rate limiter permitted → DEBUG</li>
 *   <li>Timeout timed out → WARN</li>
 *   <li>Timeout success → DEBUG</li>
 * </ul>
 */
public class HeimdallResilienceEventLogger {

    private static final Logger log = LoggerFactory.getLogger(HeimdallResilienceEventLogger.class);

    public HeimdallResilienceEventLogger(HeimdallPolicyRegistry policyRegistry) {
        policyRegistry.getAll().forEach(this::subscribe);
    }

    private void subscribe(HeimdallPolicy policy) {
        if (policy.retryExecutor() != null) {
            policy.retryExecutor().eventPublisher().subscribe(new RetryLoggingSubscriber());
        }
        if (policy.rateLimiterExecutor() != null) {
            policy.rateLimiterExecutor().eventPublisher().subscribe(new RateLimiterLoggingSubscriber());
        }
        if (policy.timeoutExecutor() != null) {
            policy.timeoutExecutor().eventPublisher().subscribe(new TimeoutLoggingSubscriber());
        }
    }

    private static class RetryLoggingSubscriber implements Flow.Subscriber<RetryEvent> {
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(RetryEvent event) {
            switch (event) {
                case RetryEvent.Attempt e ->
                        log.info("Retry [{}] attempt {}/{} after {}ms delay",
                                e.name(), e.attemptNumber(), e.maxAttempts(), e.delay().toMillis());
                case RetryEvent.Success e ->
                        log.debug("Retry [{}] succeeded after {} attempts in {}ms",
                                e.name(), e.totalAttempts(), e.totalDuration().toMillis());
                case RetryEvent.Exhausted e ->
                        log.warn("Retry [{}] exhausted after {} attempts: {}",
                                e.name(), e.totalAttempts(), e.lastException().toString());
            }
        }

        @Override public void onError(Throwable throwable) { log.error("Retry event stream error", throwable); }
        @Override public void onComplete() { log.debug("Retry event stream completed"); }
    }

    private static class RateLimiterLoggingSubscriber implements Flow.Subscriber<RateLimiterEvent> {
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(RateLimiterEvent event) {
            switch (event) {
                case RateLimiterEvent.Permitted e ->
                        log.debug("Rate limiter [{}] permitted ({} remaining)", e.name(), e.remainingPermits());
                case RateLimiterEvent.Rejected e ->
                        log.warn("Rate limiter [{}] rejected (limit: {})", e.name(), e.limitForPeriod());
            }
        }

        @Override public void onError(Throwable throwable) { log.error("Rate limiter event stream error", throwable); }
        @Override public void onComplete() { log.debug("Rate limiter event stream completed"); }
    }

    private static class TimeoutLoggingSubscriber implements Flow.Subscriber<TimeoutEvent> {
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(TimeoutEvent event) {
            switch (event) {
                case TimeoutEvent.Success e ->
                        log.debug("Timeout [{}] call completed in {}ms", e.name(), e.elapsed().toMillis());
                case TimeoutEvent.TimedOut e ->
                        log.warn("Timeout [{}] call exceeded {}ms", e.name(), e.timeout().toMillis());
            }
        }

        @Override public void onError(Throwable throwable) { log.error("Timeout event stream error", throwable); }
        @Override public void onComplete() { log.debug("Timeout event stream completed"); }
    }
}
