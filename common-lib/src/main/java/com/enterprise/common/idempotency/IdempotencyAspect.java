package com.enterprise.common.idempotency;

import com.enterprise.common.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Intercepts {@link Idempotent} methods. Workflow:
 *
 * <ol>
 *   <li>Read the {@code Idempotency-Key} header from the current request.
 *   <li>If the key already exists in the {@link IdempotencyStore} return the cached response
 *       immediately (no re-execution).
 *   <li>Otherwise proceed, serialize the response and cache it under the key.
 * </ol>
 */
@Aspect
public class IdempotencyAspect {

  private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);

  private final IdempotencyStore store;
  private final IdempotencyProperties props;

  public IdempotencyAspect(IdempotencyStore store, IdempotencyProperties props) {
    this.store = store;
    this.props = props;
  }

  /** Around advice for every {@code @Idempotent} method. */
  @Around("@annotation(com.enterprise.common.idempotency.Idempotent)")
  public Object around(ProceedingJoinPoint pjp) throws Throwable {
    HttpServletRequest request = currentRequest();
    String key = request == null ? null : request.getHeader(props.getHeaderName());
    if (key == null || key.isBlank()) {
      // No key supplied → behave like a regular call
      return pjp.proceed();
    }

    MethodSignature sig = (MethodSignature) pjp.getSignature();
    Method method = sig.getMethod();
    Idempotent ann = method.getAnnotation(Idempotent.class);
    String fullKey = (ann.name().isBlank() ? method.getName() : ann.name()) + ":" + key;

    Optional<String> cached = store.find(fullKey);
    if (cached.isPresent()) {
      log.info("Idempotency replay for key {} on {}", key, method.getName());
      return JsonUtils.fromJson(cached.get(), method.getReturnType()).orElse(null);
    }

    Object result = pjp.proceed();
    Duration ttl = ann.ttl().isBlank() ? props.getTtl() : Duration.parse(ann.ttl());
    String payload = JsonUtils.toJson(result);
    if (payload != null) {
      store.putIfAbsent(fullKey, payload, ttl);
    }
    return result;
  }

  private HttpServletRequest currentRequest() {
    var attrs = RequestContextHolder.getRequestAttributes();
    return attrs instanceof ServletRequestAttributes s ? s.getRequest() : null;
  }
}
