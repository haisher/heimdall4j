package io.github.haisher.heimdall4j.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class RingBufferTest {

    @Test
    @DisplayName("new buffer has zero count and no failure rate")
    void emptyBuffer() {
        var buffer = new RingBuffer(10);

        assertEquals(0, buffer.count());
        assertEquals(10, buffer.capacity());
        assertEquals(-1f, buffer.failureRate());
        assertFalse(buffer.isFull());
    }

    @Test
    @DisplayName("records successes correctly")
    void recordSuccess() {
        var buffer = new RingBuffer(5)
                .record(true)
                .record(true)
                .record(true);

        assertEquals(3, buffer.count());
        assertEquals(0, buffer.failureCount());
        assertEquals(0f, buffer.failureRate());
    }

    @Test
    @DisplayName("records failures correctly")
    void recordFailure() {
        var buffer = new RingBuffer(5)
                .record(false)
                .record(false)
                .record(true);

        assertEquals(3, buffer.count());
        assertEquals(2, buffer.failureCount());
        assertEquals(200f / 3, buffer.failureRate(), 0.01f);
    }

    @Test
    @DisplayName("calculates 50% failure rate")
    void fiftyPercentFailureRate() {
        var buffer = new RingBuffer(4)
                .record(true)
                .record(false)
                .record(true)
                .record(false);

        assertTrue(buffer.isFull());
        assertEquals(50f, buffer.failureRate());
    }

    @Test
    @DisplayName("wraps around evicting oldest entries")
    void wrapAround() {
        // capacity 3: record [fail, fail, success] then one more success
        var buffer = new RingBuffer(3)
                .record(false)   // [F, _, _]
                .record(false)   // [F, F, _]
                .record(true)    // [F, F, S] — full, 66% failure
                .record(true);   // [F, S, S] — evicts oldest F, 33% failure

        assertTrue(buffer.isFull());
        assertEquals(3, buffer.count());
        assertEquals(1, buffer.failureCount());
        assertEquals(100f / 3, buffer.failureRate(), 0.01f);
    }

    @Test
    @DisplayName("wraps around multiple times correctly")
    void multipleWrapArounds() {
        var buffer = new RingBuffer(2);

        // Fill: [F, F] → 100%
        buffer = buffer.record(false).record(false);
        assertEquals(100f, buffer.failureRate());

        // Wrap: [S, F] evicts first F → 50%
        buffer = buffer.record(true);
        assertEquals(50f, buffer.failureRate());

        // Wrap: [S, S] evicts second F → 0%
        buffer = buffer.record(true);
        assertEquals(0f, buffer.failureRate());
        assertEquals(0, buffer.failureCount());
    }

    @Test
    @DisplayName("capacity of 1 works correctly")
    void capacityOne() {
        var buffer = new RingBuffer(1)
                .record(false);

        assertEquals(100f, buffer.failureRate());
        assertEquals(1, buffer.count());

        buffer = buffer.record(true);
        assertEquals(0f, buffer.failureRate());
        assertEquals(1, buffer.count());
    }

    @Test
    @DisplayName("throws on invalid capacity")
    void invalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer(0));
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer(-1));
    }

    @Test
    @DisplayName("record returns new instance (immutability)")
    void immutability() {
        var original = new RingBuffer(5);
        var after = original.record(false);

        assertEquals(0, original.count());
        assertEquals(1, after.count());
        assertNotSame(original, after);
    }
}
