package io.github.haisher.heimdall4j.spring.annotation;

import io.github.haisher.heimdall4j.spring.HeimdallPolicyRegistry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * AOP aspect that intercepts methods annotated with {@link Resilient}
 * and executes them through the corresponding resilience policy.
 */
@Aspect
public class ResilientAspect {

    private final HeimdallPolicyRegistry policyRegistry;
    private final ConcurrentMap<Method, Method> fallbackCache = new ConcurrentHashMap<>();
    private static final Method NO_FALLBACK = noFallbackSentinel();

    public ResilientAspect(HeimdallPolicyRegistry policyRegistry) {
        this.policyRegistry = policyRegistry;
    }

    @Around("@annotation(resilient)")
    public Object around(ProceedingJoinPoint joinPoint, Resilient resilient) throws Throwable {
        var policyName = resilient.value();
        var policy = policyRegistry.get(policyName)
                .orElseThrow(() -> new IllegalStateException(
                        "No policy registered with name: " + policyName));

        Supplier<Object> action = () -> {
            try {
                return joinPoint.proceed();
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        };

        var fallback = resolveFallback(joinPoint);

        if (fallback != null) {
            return policy.execute(action, fallback);
        }

        return policy.execute(action);
    }

    @SuppressWarnings("unchecked")
    private Supplier<Object> resolveFallback(ProceedingJoinPoint joinPoint) {
        var method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        var fallbackMethod = fallbackCache.computeIfAbsent(method, m -> findFallbackMethod(joinPoint, m));

        if (fallbackMethod == NO_FALLBACK) {
            return null;
        }

        try {
            fallbackMethod.setAccessible(true);
            return (Supplier<Object>) fallbackMethod.invoke(joinPoint.getTarget());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke fallback method: " + fallbackMethod.getName(), e);
        }
    }

    private Method findFallbackMethod(ProceedingJoinPoint joinPoint, Method annotatedMethod) {
        var fallbackName = annotatedMethod.getName() + "Fallback";
        var targetClass = joinPoint.getTarget().getClass();

        try {
            var candidate = targetClass.getMethod(fallbackName);
            if (Supplier.class.isAssignableFrom(candidate.getReturnType())) {
                return candidate;
            }
        } catch (NoSuchMethodException _) {
            // no public method found, try declared methods
        }

        try {
            var candidate = targetClass.getDeclaredMethod(fallbackName);
            if (Supplier.class.isAssignableFrom(candidate.getReturnType())) {
                return candidate;
            }
        } catch (NoSuchMethodException _) {
            // no fallback defined — that's fine
        }

        return NO_FALLBACK;
    }

    private static Method noFallbackSentinel() {
        try {
            return ResilientAspect.class.getDeclaredMethod("noFallbackSentinel");
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }
}
