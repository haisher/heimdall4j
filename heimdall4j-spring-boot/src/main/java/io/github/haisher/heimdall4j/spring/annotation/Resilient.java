package io.github.haisher.heimdall4j.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be protected by a full resilience policy (circuit breaker + retry + timeout + rate limiter).
 *
 * <p>The policy instance is resolved by name from the {@link io.github.haisher.heimdall4j.spring.HeimdallPolicyRegistry}.
 * It must be pre-configured via properties or programmatic registration.</p>
 *
 * <p><strong>Fallback convention:</strong> If a method named {@code <methodName>Fallback}
 * exists on the same class and returns a {@code Supplier<T>}, it will be used as the fallback.</p>
 *
 * <pre>
 * &#64;Resilient("payments")
 * public PaymentResult processPayment(Order order) {
 *     return gateway.charge(order);
 * }
 *
 * public Supplier&lt;PaymentResult&gt; processPaymentFallback() {
 *     return () -&gt; PaymentResult.declined("service unavailable");
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Resilient {

    /**
     * The name of the policy instance to use.
     * Must match a registered name in the {@link io.github.haisher.heimdall4j.spring.HeimdallPolicyRegistry}.
     */
    String value();
}
