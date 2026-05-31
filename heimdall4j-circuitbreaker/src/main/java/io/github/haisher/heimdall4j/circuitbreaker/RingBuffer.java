package io.github.haisher.heimdall4j.circuitbreaker;

/**
 * Fixed-size ring buffer for tracking call outcomes.
 * Count-based sliding window — wraps around when full.
 * <p>
 * Thread-safety: This class is NOT thread-safe on its own.
 * Synchronization is handled by the CircuitBreaker via AtomicReference CAS loops.
 */
final class RingBuffer {
    private final boolean[] outcomes;
    private final int capacity;

    private int head;
    private int count;
    private int failureCount;

    RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Ring buffer capacity must be positive, got: " + capacity);
        }

        this.capacity = capacity;
        this.outcomes = new boolean[capacity];
        this.head = 0;
        this.count = 0;
        this.failureCount = 0;
    }

    private RingBuffer(boolean[] outcomes, int capacity, int head, int count, int failureCount) {
        this.outcomes = outcomes.clone();
        this.capacity = capacity;
        this.head = head;
        this.count = count;
        this.failureCount = failureCount;
    }

    /**
     * Records a call outcome.
     *
     * @param success true if the call succeeded, false if it failed
     * @return a new RingBuffer reflecting the recorded outcome
     */
    RingBuffer record(boolean success) {
        var copy = new RingBuffer(this.outcomes, this.capacity, this.head, this.count, this.failureCount);

        int writeIndex;
        if (copy.count < copy.capacity) {
            // Buffer not yet full — append at next slot
            writeIndex = (copy.head + copy.count) % copy.capacity;
            copy.count++;
        } else {
            // Buffer full — overwrite oldest (at head), advance head
            writeIndex = copy.head;
            boolean oldest = copy.outcomes[writeIndex];
            if (!oldest) {
                copy.failureCount--;
            }
            copy.head = (copy.head + 1) % copy.capacity;
        }

        copy.outcomes[writeIndex] = success;

        if (!success) {
            copy.failureCount++;
        }

        return copy;
    }

    /**
     * @return failure rate as a percentage (0-100), or -1 if no calls recorded yet
     */
    float failureRate() {
        if (count == 0) {
            return -1f;
        }

        return (failureCount * 100f) / count;
    }

    /**
     * @return number of recorded outcomes (up to capacity)
     */
    int count() {
        return count;
    }

    /**
     * @return buffer capacity
     */
    int capacity() {
        return capacity;
    }

    /**
     * @return number of failures in the current window
     */
    int failureCount() {
        return failureCount;
    }

    /**
     * @return true if the buffer has recorded at least capacity outcomes
     */
    boolean isFull() {
        return count == capacity;
    }
}
