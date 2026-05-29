package com.heimdall4j.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class StateTest {

    @Test
    @DisplayName("Closed state reports CLOSED name")
    void closedStateName() {
        var state = new State.Closed(new RingBuffer(10));
        assertEquals(StateName.CLOSED, state.name());
    }

    @Test
    @DisplayName("Open state reports OPEN name")
    void openStateName() {
        var state = new State.Open(Instant.now());
        assertEquals(StateName.OPEN, state.name());
    }

    @Test
    @DisplayName("HalfOpen state reports HALF_OPEN name")
    void halfOpenStateName() {
        var state = new State.HalfOpen(0, 0, new RingBuffer(5));
        assertEquals(StateName.HALF_OPEN, state.name());
    }

    @Test
    @DisplayName("Closed state holds ring buffer reference")
    void closedStateHoldsBuffer() {
        var buffer = new RingBuffer(10);
        var state = new State.Closed(buffer);
        assertSame(buffer, state.window());
    }

    @Test
    @DisplayName("Open state holds opened timestamp")
    void openStateHoldsTimestamp() {
        var now = Instant.now();
        var state = new State.Open(now);
        assertEquals(now, state.openedAt());
    }

    @Test
    @DisplayName("HalfOpen state tracks probe counts")
    void halfOpenStateTracksProbes() {
        var state = new State.HalfOpen(3, 1, new RingBuffer(5));
        assertEquals(3, state.probeSuccessCount());
        assertEquals(1, state.probeFailureCount());
    }

    @Test
    @DisplayName("State can be pattern-matched with switch")
    void patternMatching() {
        State state = new State.Open(Instant.now());

        String result = switch (state) {
            case State.Closed _ -> "closed";
            case State.Open _ -> "open";
            case State.HalfOpen _ -> "half_open";
        };

        assertEquals("open", result);
    }
}
