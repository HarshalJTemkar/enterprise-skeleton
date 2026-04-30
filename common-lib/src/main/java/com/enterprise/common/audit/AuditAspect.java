package com.enterprise.common.audit;

import com.enterprise.common.security.SecurityContextHelper;
import com.enterprise.common.util.JsonUtils;
import java.lang.reflect.Method;
import java.time.Instant;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * AOP aspect that wraps every {@link Auditable} method:
 * <ol>
 *   <li>captures arguments (optionally) before the call</li>
 *   <li>invokes the target</li>
 *   <li>captures the return value (optionally)</li>
 *   <li>builds an {@link AuditEvent} and pushes it through the
 *       {@link AuditEventPublisher}</li>
 *   <li>re-throws any exception after emitting a failure event</li>
 * </ol>
 */
@Aspect
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final AuditEventPublisher publisher;

    public AuditAspect(AuditEventPublisher publisher) {
        this.publisher = publisher;
    }

    /** Around advice over all {@code @Auditable} methods. */
    @Around("@annotation(com.enterprise.common.audit.Auditable)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Auditable ann = method.getAnnotation(Auditable.class);

        String who = SecurityContextHelper.currentUsername().orElse("anonymous");
        String correlationId = MDC.get("correlationId");
        String before = ann.captureArgs() ? JsonUtils.toJson(pjp.getArgs()) : null;
        String resourceId = evaluateId(ann.idExpression(), method, pjp.getArgs());

        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable ex) {
            safePublish(new AuditEvent(who, ann.action(), ann.resource(),
                    resourceId, Instant.now(), correlationId,
                    before, null, false, ex.getMessage()));
            throw ex;
        }
        String after = ann.captureResult() ? JsonUtils.toJson(result) : null;
        safePublish(new AuditEvent(who, ann.action(), ann.resource(),
                resourceId, Instant.now(), correlationId,
                before, after, true, null));
        return result;
    }

    /** Evaluate a SpEL expression against the method arguments to produce an id. */
    private String evaluateId(String expr, Method method, Object[] args) {
        if (expr == null || expr.isBlank()) return null;
        try {
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            String[] names = Binder.paramNames(method);
            for (int i = 0; i < args.length && i < names.length; i++) {
                ctx.setVariable(names[i], args[i]);
                ctx.setVariable("a" + i, args[i]);
            }
            Expression e = PARSER.parseExpression(expr);
            Object val = e.getValue(ctx);
            return val == null ? null : val.toString();
        } catch (Exception ex) {
            log.debug("Failed to evaluate audit idExpression '{}': {}", expr, ex.getMessage());
            return null;
        }
    }

    private void safePublish(AuditEvent event) {
        try {
            publisher.publish(event);
        } catch (Exception ex) {
            log.warn("Audit publisher failed: {}", ex.getMessage());
        }
    }

    /** Parameter-name binder. Extracted to a helper for easy testing. */
    static final class Binder {
        static String[] paramNames(Method m) {
            java.lang.reflect.Parameter[] ps = m.getParameters();
            String[] out = new String[ps.length];
            for (int i = 0; i < ps.length; i++) out[i] = ps[i].getName();
            return out;
        }
    }
}
