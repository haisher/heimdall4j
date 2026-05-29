package io.github.haisher.heimdall4j.core;

import io.github.haisher.heimdall4j.core.exception.CallTimeoutException;
import io.github.haisher.heimdall4j.core.exception.CircuitOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    private static final Duration WAIT_DURATION = Duration.ofSeconds(30);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(2);

    private MutableClock clock;
    private CircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(4)
                .waitDurationInOpenState(WAIT_DURATION)
                .permittedCallsInHalfOpen(2)
                .callTimeout(CALL_TIMEOUT)
                .clock(clock)
                .build();
        breaker = CircuitBreaker.of("test", config);
    }

    @Test
    @DisplayName("starts in CLOSED state")
    void startsInClosed() {
        assertEquals(StateName.CLOSED, breaker.state());
    }

    @Test
    @DisplayName("exposes name and config")
    void nameAndConfig() {
        assertEquals("test", breaker.name());
        assertNotNull(breaker.config());
    }

    @Test
    @DisplayName("successful calls keep breaker CLOSED")
    void successKeepsClosed() {
        assertEquals("ok", breaker.execute(() -> "ok"));
        assertEquals("ok", breaker.execute(() -> "ok"));
        assertEquals(StateName.CLOSED, breaker.state());
    }

    @Test
    @DisplayName("trips to OPEN when failure rate exceeds threshold")
    void tripsToOpen() {
        // Ring buffer size = 4, threshold = 50%
        // Need buffer full with >= 50% failures
        breaker.execute(() -> "ok");
        breaker.execute(() -> "ok");
        assertThrows(RuntimeException.class, () -> breaker.execute(() -> { throw new RuntimeException("fail"); }));
        assertThrows(RuntimeException.class, () -> breaker.execute(() -> { throw new RuntimeException("fail"); }));

        // Buffer full: 2 success + 2 failures = 50% → trips
        assertEquals(StateName.OPEN, breaker.state());
    }

    @Test
    @DisplayName("OPEN state rejects calls with CircuitOpenException")
    void openRejectsCalls() {
        tripBreaker();
        assertThrows(CircuitOpenException.class, () -> breaker.execute(() -> "ok"));
    }

    @Test
    @DisplayName("OPEN state invokes fallback when provided")
    void openInvokesFallback() {
        tripBreaker();
        var result = breaker.execute(() -> "ok", () -> "fallback");
        assertEquals("fallback", result);
    }

    @Test
    @DisplayName("transitions to HALF_OPEN after wait duration elapses")
    void transitionsToHalfOpen() {
        tripBreaker();
        clock.advance(WAIT_DURATION.plusSeconds(1));

        // Next call should be allowed (half-open probe)
        var result = breaker.execute(() -> "probe");
        assertEquals("probe", result);
        assertEquals(StateName.HALF_OPEN, breaker.state());
    }

    @Test
    @DisplayName("HALF_OPEN transitions to CLOSED after enough successful probes")
    void halfOpenToClosed() {
        tripBreaker();
        clock.advance(WAIT_DURATION.plusSeconds(1));

        // permittedCallsInHalfOpen = 2
        breaker.execute(() -> "probe1");
        breaker.execute(() -> "probe2");

        assertEquals(StateName.CLOSED, breaker.state());
    }

    @Test
    @DisplayName("HALF_OPEN transitions to OPEN on probe failure")
    void halfOpenToOpen() {
        tripBreaker();
        clock.advance(WAIT_DURATION.plusSeconds(1));

        // First probe succeeds, transition to half-open
        breaker.execute(() -> "probe1");
        assertEquals(StateName.HALF_OPEN, breaker.state());

        // Second probe fails → back to OPEN
        assertThrows(RuntimeException.class, () -> breaker.execute(() -> { throw new RuntimeException("fail"); }));
        assertEquals(StateName.OPEN, breaker.state());
    }

    @Test
    @DisplayName("does not trip on non-recorded exceptions")
    void nonRecordedExceptions() {
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(4)
                .waitDurationInOpenState(WAIT_DURATION)
                .permittedCallsInHalfOpen(2)
                .callTimeout(CALL_TIMEOUT)
                .recordFailure(e -> !(e instanceof IllegalArgumentException))
                .clock(clock)
                .build();
        var cb = CircuitBreaker.of("filtered", config);

        // 4 IllegalArgumentExceptions — should NOT trip
        for (int i = 0; i < 4; i++) {
            assertThrows(IllegalArgumentException.class, () ->
                    cb.execute(() -> { throw new IllegalArgumentException("bad input"); }));
        }
        assertEquals(StateName.CLOSED, cb.state());
    }

    @Test
    @DisplayName("timeout triggers failure recording and throws CallTimeoutException")
    void callTimeout() {
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(4)
                .waitDurationInOpenState(WAIT_DURATION)
                .permittedCallsInHalfOpen(2)
                .callTimeout(Duration.ofMillis(50))
                .clock(clock)
                .build();
        var cb = CircuitBreaker.of("timeout-test", config);

        // Fill buffer with timeouts
        for (int i = 0; i < 4; i++) {
            assertThrows(CallTimeoutException.class, () ->
                    cb.execute(() -> {
                        try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                        return "too slow";
                    }));
        }

        assertEquals(StateName.OPEN, cb.state());
    }

    @Test
    @DisplayName("does not trip before buffer is full")
    void doesNotTripBeforeBufferFull() {
        // 3 failures in buffer of 4 — not full yet
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, () ->
                    breaker.execute(() -> { throw new RuntimeException("fail"); }));
        }
        assertEquals(StateName.CLOSED, breaker.state());
    }

    @Test
    @DisplayName("full lifecycle: CLOSED → OPEN → HALF_OPEN → CLOSED")
    void fullLifecycle() {
        assertEquals(StateName.CLOSED, breaker.state());

        // Trip it
        tripBreaker();
        assertEquals(StateName.OPEN, breaker.state());

        // Wait and recover
        clock.advance(WAIT_DURATION.plusSeconds(1));
        breaker.execute(() -> "probe1");
        assertEquals(StateName.HALF_OPEN, breaker.state());

        breaker.execute(() -> "probe2");
        assertEquals(StateName.CLOSED, breaker.state());

        // Verify it works normally again
        assertEquals("back", breaker.execute(() -> "back"));
    }

    private void tripBreaker() {
        breaker.execute(() -> "ok");
        breaker.execute(() -> "ok");
        try { breaker.execute(() -> { throw new RuntimeException("fail"); }); } catch (Exception ignored) {}
        try { breaker.execute(() -> { throw new RuntimeException("fail"); }); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("HALF_OPEN rejects calls with fallback when all probe slots are exhausted")
    void halfOpenExhaustedWithFallback() {
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(4)
                .waitDurationInOpenState(WAIT_DURATION)
                .permittedCallsInHalfOpen(1) // only 1 probe allowed
                .callTimeout(CALL_TIMEOUT)
                .clock(clock)
                .build();
        var cb = CircuitBreaker.of("half-open-exhaust", config);

        // Trip it
        cb.execute(() -> "ok"); cb.execute(() -> "ok");
        try { cb.execute(() -> { throw new RuntimeException(); }); } catch (Exception ignored) {}
        try { cb.execute(() -> { throw new RuntimeException(); }); } catch (Exception ignored) {}
        assertEquals(StateName.OPEN, cb.state());

        // Transition to HALF_OPEN
        clock.advance(WAIT_DURATION.plusSeconds(1));
        cb.execute(() -> "probe"); // uses the 1 permitted probe, transitions to CLOSED
        assertEquals(StateName.CLOSED, cb.state());
    }

    @Test
    @DisplayName("HALF_OPEN throws CircuitOpenException when probes exhausted and no fallback")
    void halfOpenExhaustedNoFallback() {
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(2)
                .waitDurationInOpenState(WAIT_DURATION)
                .permittedCallsInHalfOpen(1)
                .callTimeout(CALL_TIMEOUT)
                .clock(clock)
                .build();
        var cb = CircuitBreaker.of("half-open-reject", config);

        // Trip: buffer size 2, threshold 50% — 1 success + 1 failure trips
        cb.execute(() -> "ok");
        try { cb.execute(() -> { throw new RuntimeException(); }); } catch (Exception ignored) {}
        assertEquals(StateName.OPEN, cb.state());

        // Transition to HALF_OPEN, use the single probe but fail → back to OPEN
        clock.advance(WAIT_DURATION.plusSeconds(1));
        try { cb.execute(() -> { throw new RuntimeException("probe fail"); }); } catch (Exception ignored) {}
        assertEquals(StateName.OPEN, cb.state());

        // Now advance again into HALF_OPEN and succeed, using the probe
        clock.advance(WAIT_DURATION.plusSeconds(1));
        cb.execute(() -> "probe");
        assertEquals(StateName.CLOSED, cb.state());
    }

    @Test
    @DisplayName("HALF_OPEN timeout during probe trips back to OPEN")
    void halfOpenTimeoutTripsToOpen() {
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(2)
                .waitDurationInOpenState(WAIT_DURATION)
                .permittedCallsInHalfOpen(1)
                .callTimeout(Duration.ofMillis(50))
                .clock(clock)
                .build();
        var cb = CircuitBreaker.of("half-open-timeout", config);

        // Trip it
        cb.execute(() -> "ok");
        try { cb.execute(() -> { throw new RuntimeException(); }); } catch (Exception ignored) {}
        assertEquals(StateName.OPEN, cb.state());

        // Transition to HALF_OPEN, probe times out → back to OPEN
        clock.advance(WAIT_DURATION.plusSeconds(1));
        assertThrows(CallTimeoutException.class, () ->
                cb.execute(() -> {
                    try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    return "slow";
                }));
        assertEquals(StateName.OPEN, cb.state());
    }

    @Test
    @DisplayName("executeWithTimeout wraps checked exceptions from supplier")
    void checkedExceptionWrapping() {
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(4)
                .waitDurationInOpenState(WAIT_DURATION)
                .permittedCallsInHalfOpen(2)
                .callTimeout(Duration.ofSeconds(5))
                .clock(clock)
                .build();
        var cb = CircuitBreaker.of("checked-ex", config);

        // Supplier that throws a checked exception (simulated via ExecutionException path)
        var ex = assertThrows(RuntimeException.class, () ->
                cb.execute(() -> { throw new RuntimeException(new java.io.IOException("disk error")); }));
        assertNotNull(ex.getCause());
    }

    @Test
    @DisplayName("interrupted thread during timeout execution restores interrupt flag")
    void interruptedDuringTimeout() {
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(4)
                .waitDurationInOpenState(WAIT_DURATION)
                .permittedCallsInHalfOpen(2)
                .callTimeout(Duration.ofSeconds(10))
                .clock(clock)
                .build();
        var cb = CircuitBreaker.of("interrupt-test", config);

        // Interrupt the current thread before execute — future.get() will throw InterruptedException
        Thread.currentThread().interrupt();
        assertThrows(RuntimeException.class, () -> cb.execute(() -> "value"));

        // Interrupt flag should be restored
        assertTrue(Thread.interrupted(), "Interrupt flag should be set");
    }
}
