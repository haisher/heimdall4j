package io.github.haisher.heimdall4j.retry;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RetryConfigTest {

    @Test
    void builderCreatesConfigWithDefaults() {
        var config = RetryConfig.builder().build();
        assertEquals(3, config.maxAttempts());
        assertEquals(Duration.ofMillis(500), config.delay());
        assertEquals(1.0, config.multiplier());
        assertNotNull(config.retryOn());
    }

    @Test
    void fixedDelayFactory() {
        var config = RetryConfig.fixedDelay(5, Duration.ofMillis(100));
        assertEquals(5, config.maxAttempts());
        assertEquals(Duration.ofMillis(100), config.delay());
        assertEquals(1.0, config.multiplier());
        assertFalse(config.isExponential());
    }

    @Test
    void exponentialBackoffFactory() {
        var config = RetryConfig.exponentialBackoff(4, Duration.ofMillis(200), 2.0);
        assertEquals(4, config.maxAttempts());
        assertEquals(Duration.ofMillis(200), config.delay());
        assertEquals(2.0, config.multiplier());
        assertTrue(config.isExponential());
    }

    @Test
    void delayForAttemptFixedDelay() {
        var config = RetryConfig.fixedDelay(3, Duration.ofMillis(100));
        assertEquals(Duration.ofMillis(100), config.delayForAttempt(0));
        assertEquals(Duration.ofMillis(100), config.delayForAttempt(1));
        assertEquals(Duration.ofMillis(100), config.delayForAttempt(2));
    }

    @Test
    void delayForAttemptExponentialBackoff() {
        var config = RetryConfig.exponentialBackoff(4, Duration.ofMillis(100), 2.0);
        assertEquals(Duration.ofMillis(100), config.delayForAttempt(0));
        assertEquals(Duration.ofMillis(200), config.delayForAttempt(1));
        assertEquals(Duration.ofMillis(400), config.delayForAttempt(2));
        assertEquals(Duration.ofMillis(800), config.delayForAttempt(3));
    }

    @Test
    void rejectsInvalidMaxAttempts() {
        assertThrows(IllegalArgumentException.class, () -> RetryConfig.fixedDelay(0, Duration.ofMillis(100)));
    }

    @Test
    void rejectsNegativeDelay() {
        assertThrows(IllegalArgumentException.class, () -> RetryConfig.fixedDelay(3, Duration.ofMillis(-1)));
    }

    @Test
    void rejectsMultiplierBelowOne() {
        assertThrows(IllegalArgumentException.class, () ->
                RetryConfig.builder().multiplier(0.5).build());
    }

    @Test
    void builderCustomRetryOnPredicate() {
        var config = RetryConfig.builder()
                .retryOn(e -> e instanceof IllegalStateException)
                .build();
        assertTrue(config.retryOn().test(new IllegalStateException()));
        assertFalse(config.retryOn().test(new NullPointerException()));
    }
}
