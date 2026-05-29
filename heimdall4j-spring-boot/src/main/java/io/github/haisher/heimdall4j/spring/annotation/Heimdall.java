package io.github.haisher.heimdall4j.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be protected by a circuit breaker.
 *
 * <p>The circuit breaker instance is resolved by name from the {@link io.github.haisher.heimdall4j.spring.HeimdallRegistry}.
 * It must be pre-configured via properties or programmatic registration.</p>
 *
 * <p><strong>Fallback convention:</strong> If a method named {@code <methodName>Fallback}
 * exists on the same class and returns a {@code Supplier<T>}, it will be used as the fallback
 * when the circuit is open.</p>
 *
 * <pre>
 * &#64;Heimdall("payments")
 * public PaymentResult processPayment(Order order) {
 *     return gateway.charge(order);
 * }
 *
 * // Convention: same name + "Fallback" suffix, returns Supplier&lt;T&gt;
 * public Supplier&lt;PaymentResult&gt; processPaymentFallback() {
 *     return () -&gt; PaymentResult.declined("service unavailable");
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Heimdall {

    /**
     * The name of the circuit breaker instance to use.
     * Must match a registered name in the {@link io.github.haisher.heimdall4j.spring.HeimdallRegistry}.
     */
    String value();
}
