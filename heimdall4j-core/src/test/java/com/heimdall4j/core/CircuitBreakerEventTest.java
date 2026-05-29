package com.heimdall4j.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerEventTest {

    private static final Duration WAIT_DURATION = Duration.ofSeconds(30);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(2);

    private MutableClock clock;
    private CircuitBreaker breaker;
    private TestSubscriber subscriber;

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
        subscriber = new TestSubscriber();
        breaker.eventPublisher().subscribe(subscriber);
    }

    @Test
    @DisplayName("emits CallSuccess on successful call")
    void emitsCallSuccess() throws InterruptedException {
        breaker.execute(() -> "ok");

        subscriber.awaitEvents(1);
        assertEquals(1, subscriber.events.size());
        assertInstanceOf(CircuitBreakerEvent.CallSuccess.class, subscriber.events.getFirst());

        var event = (CircuitBreakerEvent.CallSuccess) subscriber.events.getFirst();
        assertEquals("test", event.circuitBreakerName());
        assertNotNull(event.elapsed());
    }

    @Test
    @DisplayName("emits CallFailure on failed call")
    void emitsCallFailure() throws InterruptedException {
        assertThrows(RuntimeException.class, () ->
                breaker.execute(() -> { throw new RuntimeException("boom"); }));

        subscriber.awaitEvents(1);
        assertEquals(1, subscriber.events.size());
        assertInstanceOf(CircuitBreakerEvent.CallFailure.class, subscriber.events.getFirst());

        var event = (CircuitBreakerEvent.CallFailure) subscriber.events.getFirst();
        assertEquals("test", event.circuitBreakerName());
        assertEquals("boom", event.cause().getMessage());
    }

    @Test
    @DisplayName("emits CallTimeout on timed-out call")
    void emitsCallTimeout() throws InterruptedException {
        var config = CircuitBreakerConfig.builder()
                .callTimeout(Duration.ofMillis(50))
                .ringBufferSize(4)
                .clock(clock)
                .build();
        var cb = CircuitBreaker.of("timeout", config);
        var sub = new TestSubscriber();
        cb.eventPublisher().subscribe(sub);

        assertThrows(Exception.class, () ->
                cb.execute(() -> {
                    try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    return "slow";
                }));

        sub.awaitEvents(1);
        assertEquals(1, sub.events.size());
        assertInstanceOf(CircuitBreakerEvent.CallTimeout.class, sub.events.getFirst());
    }

    @Test
    @DisplayName("emits StateTransition CLOSED → OPEN when breaker trips")
    void emitsClosedToOpen() throws InterruptedException {
        // Fill buffer: 2 success + 2 failure = 50% → trips
        breaker.execute(() -> "ok");
        breaker.execute(() -> "ok");
        try { breaker.execute(() -> { throw new RuntimeException("fail"); }); } catch (Exception ignored) {}
        try { breaker.execute(() -> { throw new RuntimeException("fail"); }); } catch (Exception ignored) {}

        subscriber.awaitEvents(5); // 2 success + 2 failure + 1 state transition

        var transitions = subscriber.events.stream()
                .filter(e -> e instanceof CircuitBreakerEvent.StateTransition)
                .map(e -> (CircuitBreakerEvent.StateTransition) e)
                .toList();

        assertEquals(1, transitions.size());
        assertEquals(StateName.CLOSED, transitions.getFirst().from());
        assertEquals(StateName.OPEN, transitions.getFirst().to());
    }

    @Test
    @DisplayName("emits StateTransition OPEN → HALF_OPEN after wait")
    void emitsOpenToHalfOpen() throws InterruptedException {
        tripBreaker();
        subscriber.events.clear();

        clock.advance(WAIT_DURATION.plusSeconds(1));
        breaker.execute(() -> "probe");

        subscriber.awaitEvents(2); // state transition + call success

        var transitions = subscriber.events.stream()
                .filter(e -> e instanceof CircuitBreakerEvent.StateTransition)
                .map(e -> (CircuitBreakerEvent.StateTransition) e)
                .toList();

        assertEquals(1, transitions.size());
        assertEquals(StateName.OPEN, transitions.getFirst().from());
        assertEquals(StateName.HALF_OPEN, transitions.getFirst().to());
    }

    @Test
    @DisplayName("emits StateTransition HALF_OPEN → CLOSED after probes succeed")
    void emitsHalfOpenToClosed() throws InterruptedException {
        tripBreaker();
        clock.advance(WAIT_DURATION.plusSeconds(1));

        // Wait for all trip events to be delivered, then clear
        subscriber.awaitEvents(5);
        subscriber.events.clear();

        breaker.execute(() -> "probe1");
        breaker.execute(() -> "probe2");

        subscriber.awaitEvents(4); // OPEN→HALF_OPEN + success + success + HALF_OPEN→CLOSED

        var transitions = subscriber.events.stream()
                .filter(e -> e instanceof CircuitBreakerEvent.StateTransition)
                .map(e -> (CircuitBreakerEvent.StateTransition) e)
                .toList();

        assertTrue(transitions.stream().anyMatch(t ->
                t.from() == StateName.HALF_OPEN && t.to() == StateName.CLOSED));
    }

    @Test
    @DisplayName("no events emitted after close()")
    void noEventsAfterClose() throws InterruptedException {
        breaker.close();
        breaker.execute(() -> "ok");

        Thread.sleep(50);
        assertTrue(subscriber.events.isEmpty());
    }

    private void tripBreaker() {
        breaker.execute(() -> "ok");
        breaker.execute(() -> "ok");
        try { breaker.execute(() -> { throw new RuntimeException("fail"); }); } catch (Exception ignored) {}
        try { breaker.execute(() -> { throw new RuntimeException("fail"); }); } catch (Exception ignored) {}
    }

    /**
     * Test subscriber that collects events into a thread-safe list.
     */
    static final class TestSubscriber implements Flow.Subscriber<CircuitBreakerEvent> {
        final CopyOnWriteArrayList<CircuitBreakerEvent> events = new CopyOnWriteArrayList<>();
        private Flow.Subscription subscription;
        private final CountDownLatch subscribedLatch = new CountDownLatch(1);

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
            subscribedLatch.countDown();
        }

        @Override
        public void onNext(CircuitBreakerEvent item) {
            events.add(item);
        }

        @Override
        public void onError(Throwable throwable) {}

        @Override
        public void onComplete() {}

        void awaitEvents(int count) throws InterruptedException {
            subscribedLatch.await(1, TimeUnit.SECONDS);
            // Give SubmissionPublisher time to deliver
            long deadline = System.currentTimeMillis() + 2000;
            while (events.size() < count && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
        }
    }
}
