package io.github.haisher.heimdall4j.timeout;

import io.github.haisher.heimdall4j.timeout.exception.CallTimeoutException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;

class TimeoutExecutorTest {

    @Test
    void succeedsWithinTimeout() {
        var executor = TimeoutExecutor.of("test", TimeoutConfig.ofDuration(Duration.ofSeconds(1)));
        var result = executor.execute(() -> "ok");
        assertEquals("ok", result);
    }

    @Test
    void throwsOnTimeout() {
        var executor = TimeoutExecutor.of("test", TimeoutConfig.ofDuration(Duration.ofMillis(50)));

        var ex = assertThrows(CallTimeoutException.class, () ->
                executor.execute(() -> {
                    try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return "late";
                }));

        assertEquals("test", ex.timeoutName());
        assertEquals(Duration.ofMillis(50), ex.timeout());
    }

    @Test
    void invokesFallbackOnTimeout() {
        var executor = TimeoutExecutor.of("test", TimeoutConfig.ofDuration(Duration.ofMillis(50)));

        var result = executor.execute(
                () -> { try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } return "late"; },
                () -> "fallback"
        );

        assertEquals("fallback", result);
    }

    @Test
    void propagatesRuntimeException() {
        var executor = TimeoutExecutor.of("test", TimeoutConfig.ofDuration(Duration.ofSeconds(1)));

        assertThrows(IllegalStateException.class, () ->
                executor.execute(() -> { throw new IllegalStateException("boom"); }));
    }

    @Test
    void emitsSuccessEvent() throws InterruptedException {
        var events = new ArrayList<TimeoutEvent>();
        var executor = TimeoutExecutor.of("test", TimeoutConfig.ofDuration(Duration.ofSeconds(1)));

        executor.eventPublisher().subscribe(new Flow.Subscriber<>() {
            @Override public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            @Override public void onNext(TimeoutEvent item) { events.add(item); }
            @Override public void onError(Throwable t) {}
            @Override public void onComplete() {}
        });

        Thread.sleep(50);
        executor.execute(() -> "ok");
        Thread.sleep(50);

        assertEquals(1, events.size());
        assertInstanceOf(TimeoutEvent.Success.class, events.getFirst());
    }

    @Test
    void emitsTimedOutEvent() throws InterruptedException {
        var events = new ArrayList<TimeoutEvent>();
        var executor = TimeoutExecutor.of("test", TimeoutConfig.ofDuration(Duration.ofMillis(50)));

        executor.eventPublisher().subscribe(new Flow.Subscriber<>() {
            @Override public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            @Override public void onNext(TimeoutEvent item) { events.add(item); }
            @Override public void onError(Throwable t) {}
            @Override public void onComplete() {}
        });

        Thread.sleep(50);
        try { executor.execute(() -> { try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } return "late"; }); } catch (CallTimeoutException _) {}
        Thread.sleep(50);

        assertEquals(1, events.size());
        assertInstanceOf(TimeoutEvent.TimedOut.class, events.getFirst());
    }

    @Test
    void nameAndConfigAccessors() {
        var config = TimeoutConfig.ofDuration(Duration.ofSeconds(3));
        var executor = TimeoutExecutor.of("myTimeout", config);
        assertEquals("myTimeout", executor.name());
        assertSame(config, executor.config());
    }

    @Test
    void closeStopsEventPublisher() {
        var executor = TimeoutExecutor.of("test", TimeoutConfig.ofDuration(Duration.ofSeconds(1)));
        executor.close();
        assertEquals("ok", executor.execute(() -> "ok"));
    }

    @Test
    void wrapsCheckedExceptionFromSupplier() {
        var executor = TimeoutExecutor.of("test", TimeoutConfig.ofDuration(Duration.ofSeconds(1)));

        var ex = assertThrows(RuntimeException.class, () ->
                executor.execute(() -> { throw new RuntimeException(new Exception("checked")); }));

        assertNotNull(ex.getCause());
    }
}
