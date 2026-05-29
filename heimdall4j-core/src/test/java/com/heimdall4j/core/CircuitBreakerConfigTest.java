package com.heimdall4j.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Clock;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerConfigTest {

    @Test
    @DisplayName("builder creates config with defaults")
    void defaultConfig() {
        var config = CircuitBreakerConfig.builder().build();

        assertEquals(50, config.failureRateThreshold());
        assertEquals(100, config.ringBufferSize());
        assertEquals(Duration.ofSeconds(30), config.waitDurationInOpenState());
        assertEquals(10, config.permittedCallsInHalfOpen());
        assertEquals(Duration.ofSeconds(5), config.callTimeout());
        assertNotNull(config.recordFailure());
        assertNotNull(config.clock());
    }

    @Test
    @DisplayName("ofDefaults() creates config equivalent to builder defaults")
    void ofDefaults() {
        var config = CircuitBreakerConfig.ofDefaults();

        assertEquals(50, config.failureRateThreshold());
        assertEquals(100, config.ringBufferSize());
        assertEquals(Duration.ofSeconds(30), config.waitDurationInOpenState());
        assertEquals(10, config.permittedCallsInHalfOpen());
        assertEquals(Duration.ofSeconds(5), config.callTimeout());
        assertNotNull(config.recordFailure());
        assertNotNull(config.clock());
    }

    @Test
    @DisplayName("builder accepts custom values")
    void customConfig() {
        var clock = Clock.systemUTC();
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(75)
                .ringBufferSize(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedCallsInHalfOpen(5)
                .callTimeout(Duration.ofSeconds(3))
                .recordFailure(e -> e instanceof RuntimeException)
                .clock(clock)
                .build();

        assertEquals(75, config.failureRateThreshold());
        assertEquals(50, config.ringBufferSize());
        assertEquals(Duration.ofSeconds(60), config.waitDurationInOpenState());
        assertEquals(5, config.permittedCallsInHalfOpen());
        assertEquals(Duration.ofSeconds(3), config.callTimeout());
        assertSame(clock, config.clock());
    }

    @Test
    @DisplayName("rejects invalid failure rate threshold")
    void invalidThreshold() {
        assertThrows(IllegalArgumentException.class, () ->
                CircuitBreakerConfig.builder().failureRateThreshold(0).build());
        assertThrows(IllegalArgumentException.class, () ->
                CircuitBreakerConfig.builder().failureRateThreshold(101).build());
    }

    @Test
    @DisplayName("rejects invalid ring buffer size")
    void invalidRingBufferSize() {
        assertThrows(IllegalArgumentException.class, () ->
                CircuitBreakerConfig.builder().ringBufferSize(0).build());
    }

    @Test
    @DisplayName("rejects null or non-positive durations")
    void invalidDurations() {
        assertThrows(NullPointerException.class, () ->
                CircuitBreakerConfig.builder().waitDurationInOpenState(null).build());
        assertThrows(IllegalArgumentException.class, () ->
                CircuitBreakerConfig.builder().waitDurationInOpenState(Duration.ZERO).build());
        assertThrows(NullPointerException.class, () ->
                CircuitBreakerConfig.builder().callTimeout(null).build());
        assertThrows(IllegalArgumentException.class, () ->
                CircuitBreakerConfig.builder().callTimeout(Duration.ofSeconds(-1)).build());
    }

    @Test
    @DisplayName("rejects null predicate and clock")
    void nullChecks() {
        assertThrows(NullPointerException.class, () ->
                CircuitBreakerConfig.builder().recordFailure(null).build());
        assertThrows(NullPointerException.class, () ->
                CircuitBreakerConfig.builder().clock(null).build());
    }

    @Test
    @DisplayName("default recordFailure predicate records all exceptions")
    void defaultPredicateRecordsAll() {
        var config = CircuitBreakerConfig.builder().build();
        assertTrue(config.recordFailure().test(new RuntimeException()));
        assertTrue(config.recordFailure().test(new IllegalArgumentException()));
    }
}
