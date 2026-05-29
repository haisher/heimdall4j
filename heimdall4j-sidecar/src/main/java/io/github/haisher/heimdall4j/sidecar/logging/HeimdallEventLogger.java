package io.github.haisher.heimdall4j.sidecar.logging;

import io.github.haisher.heimdall4j.core.CircuitBreaker;
import io.github.haisher.heimdall4j.core.CircuitBreakerEvent;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;

/**
 * Subscribes to circuit breaker events and logs them via SLF4J.
 *
 * <p>Log levels:</p>
 * <ul>
 *   <li>StateTransition → WARN</li>
 *   <li>CallFailure → WARN</li>
 *   <li>CallTimeout → WARN</li>
 *   <li>CallSuccess → DEBUG</li>
 * </ul>
 */
public class HeimdallEventLogger {

    private static final Logger log = LoggerFactory.getLogger(HeimdallEventLogger.class);

    public HeimdallEventLogger(HeimdallRegistry registry) {
        registry.getAll().forEach(this::subscribe);
    }

    private void subscribe(CircuitBreaker breaker) {
        breaker.eventPublisher().subscribe(new LoggingSubscriber());
    }

    private static class LoggingSubscriber implements Flow.Subscriber<CircuitBreakerEvent> {

        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(CircuitBreakerEvent event) {
            switch (event) {
                case CircuitBreakerEvent.StateTransition e ->
                        log.warn("Circuit breaker [{}] state transition: {} → {}",
                                e.circuitBreakerName(), e.from(), e.to());
                case CircuitBreakerEvent.CallSuccess e ->
                        log.debug("Circuit breaker [{}] call succeeded in {}ms",
                                e.circuitBreakerName(), e.elapsed().toMillis());
                case CircuitBreakerEvent.CallFailure e ->
                        log.warn("Circuit breaker [{}] call failed in {}ms: {}",
                                e.circuitBreakerName(), e.elapsed().toMillis(), e.cause().toString());
                case CircuitBreakerEvent.CallTimeout e ->
                        log.warn("Circuit breaker [{}] call timed out (configured: {}ms)",
                                e.circuitBreakerName(), e.configuredTimeout().toMillis());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("Circuit breaker event stream error", throwable);
        }

        @Override
        public void onComplete() {
            log.debug("Circuit breaker event stream completed");
        }
    }
}
