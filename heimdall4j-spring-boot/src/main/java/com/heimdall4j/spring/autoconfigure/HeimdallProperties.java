package com.heimdall4j.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Heimdall4j circuit breakers.
 *
 * <pre>
 * heimdall4j:
 *   instances:
 *     payments:
 *       failure-rate-threshold: 50
 *       ring-buffer-size: 100
 *       wait-duration: 30s
 *       permitted-calls-in-half-open: 10
 *       call-timeout: 2s
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

        private int failureRateThreshold = 50;
        private int ringBufferSize = 100;
        private Duration waitDuration = Duration.ofSeconds(30);
        private int permittedCallsInHalfOpen = 10;
        private Duration callTimeout = Duration.ofSeconds(5);

        public int getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(int failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public int getRingBufferSize() {
            return ringBufferSize;
        }

        public void setRingBufferSize(int ringBufferSize) {
            this.ringBufferSize = ringBufferSize;
        }

        public Duration getWaitDuration() {
            return waitDuration;
        }

        public void setWaitDuration(Duration waitDuration) {
            this.waitDuration = waitDuration;
        }

        public int getPermittedCallsInHalfOpen() {
            return permittedCallsInHalfOpen;
        }

        public void setPermittedCallsInHalfOpen(int permittedCallsInHalfOpen) {
            this.permittedCallsInHalfOpen = permittedCallsInHalfOpen;
        }

        public Duration getCallTimeout() {
            return callTimeout;
        }

        public void setCallTimeout(Duration callTimeout) {
            this.callTimeout = callTimeout;
        }
    }
}
