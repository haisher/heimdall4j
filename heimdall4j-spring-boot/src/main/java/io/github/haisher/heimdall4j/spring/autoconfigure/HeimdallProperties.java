package io.github.haisher.heimdall4j.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Heimdall4j resilience policies.
 *
 * <pre>
 * heimdall4j:
 *   instances:
 *     payments:
 *       circuit-breaker:
 *         failure-rate-threshold: 50
 *         ring-buffer-size: 100
 *         wait-duration: 30s
 *         permitted-calls-in-half-open: 10
 *       retry:
 *         max-attempts: 3
 *         delay: 200ms
 *         multiplier: 2.0
 *       rate-limiter:
 *         limit-for-period: 100
 *         refresh-period: 1s
 *       timeout:
 *         duration: 2s
 * </pre>
 */
@ConfigurationProperties(prefix = "heimdall4j")
public class HeimdallProperties {

    private Map<String, InstanceProperties> instances = new HashMap<>();

    public Map<String, InstanceProperties> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, InstanceProperties> instances) {
        this.instances = instances;
    }

    public static class InstanceProperties {

        private CircuitBreakerProperties circuitBreaker;
        private RetryProperties retry;
        private RateLimiterProperties rateLimiter;
        private TimeoutProperties timeout;

        // Legacy flat properties for backward compatibility
        private Integer failureRateThreshold;
        private Integer ringBufferSize;
        private Duration waitDuration;
        private Integer permittedCallsInHalfOpen;
        private Duration callTimeout;

        public CircuitBreakerProperties getCircuitBreaker() {
            return circuitBreaker;
        }

        public void setCircuitBreaker(CircuitBreakerProperties circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
        }

        public RetryProperties getRetry() {
            return retry;
        }

        public void setRetry(RetryProperties retry) {
            this.retry = retry;
        }

        public RateLimiterProperties getRateLimiter() {
            return rateLimiter;
        }

        public void setRateLimiter(RateLimiterProperties rateLimiter) {
            this.rateLimiter = rateLimiter;
        }

        public TimeoutProperties getTimeout() {
            return timeout;
        }

        public void setTimeout(TimeoutProperties timeout) {
            this.timeout = timeout;
        }

        // Legacy accessors for backward compatibility
        public Integer getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(Integer failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public Integer getRingBufferSize() {
            return ringBufferSize;
        }

        public void setRingBufferSize(Integer ringBufferSize) {
            this.ringBufferSize = ringBufferSize;
        }

        public Duration getWaitDuration() {
            return waitDuration;
        }

        public void setWaitDuration(Duration waitDuration) {
            this.waitDuration = waitDuration;
        }

        public Integer getPermittedCallsInHalfOpen() {
            return permittedCallsInHalfOpen;
        }

        public void setPermittedCallsInHalfOpen(Integer permittedCallsInHalfOpen) {
            this.permittedCallsInHalfOpen = permittedCallsInHalfOpen;
        }

        public Duration getCallTimeout() {
            return callTimeout;
        }

        public void setCallTimeout(Duration callTimeout) {
            this.callTimeout = callTimeout;
        }

        /**
         * Returns true if this instance uses the legacy flat circuit-breaker-only config.
         */
        public boolean isLegacyConfig() {
            return circuitBreaker == null && retry == null && rateLimiter == null && timeout == null
                    && (failureRateThreshold != null || ringBufferSize != null || waitDuration != null
                    || permittedCallsInHalfOpen != null || callTimeout != null);
        }
    }

    public static class CircuitBreakerProperties {
        private int failureRateThreshold = 50;
        private int ringBufferSize = 100;
        private Duration waitDuration = Duration.ofSeconds(30);
        private int permittedCallsInHalfOpen = 10;
        private Duration callTimeout = Duration.ofSeconds(5);

        public int getFailureRateThreshold() { return failureRateThreshold; }
        public void setFailureRateThreshold(int v) { this.failureRateThreshold = v; }
        public int getRingBufferSize() { return ringBufferSize; }
        public void setRingBufferSize(int v) { this.ringBufferSize = v; }
        public Duration getWaitDuration() { return waitDuration; }
        public void setWaitDuration(Duration v) { this.waitDuration = v; }
        public int getPermittedCallsInHalfOpen() { return permittedCallsInHalfOpen; }
        public void setPermittedCallsInHalfOpen(int v) { this.permittedCallsInHalfOpen = v; }
        public Duration getCallTimeout() { return callTimeout; }
        public void setCallTimeout(Duration v) { this.callTimeout = v; }
    }

    public static class RetryProperties {
        private int maxAttempts = 3;
        private Duration delay = Duration.ofMillis(500);
        private double multiplier = 1.0;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { this.maxAttempts = v; }
        public Duration getDelay() { return delay; }
        public void setDelay(Duration v) { this.delay = v; }
        public double getMultiplier() { return multiplier; }
        public void setMultiplier(double v) { this.multiplier = v; }
    }

    public static class RateLimiterProperties {
        private int limitForPeriod = 50;
        private Duration refreshPeriod = Duration.ofSeconds(1);

        public int getLimitForPeriod() { return limitForPeriod; }
        public void setLimitForPeriod(int v) { this.limitForPeriod = v; }
        public Duration getRefreshPeriod() { return refreshPeriod; }
        public void setRefreshPeriod(Duration v) { this.refreshPeriod = v; }
    }

    public static class TimeoutProperties {
        private Duration duration = Duration.ofSeconds(5);

        public Duration getDuration() { return duration; }
        public void setDuration(Duration v) { this.duration = v; }
    }
}
