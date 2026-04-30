package com.enterprise.gateway.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Default fallback endpoint forwarded to by the gateway's CircuitBreaker filter when a downstream
 * is unreachable / slow / has tripped its breaker.
 *
 * <p>Returns a stable RFC 7807-shaped JSON body so clients can branch on the structured payload
 * instead of trying to parse different upstream error formats.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

  /** Generic fallback for any route that points here. */
  @GetMapping(produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
  public Mono<ResponseEntity<Map<String, Object>>> fallback() {
    Map<String, Object> body =
        Map.of(
            "type", "https://errors.enterprise.com/ERR-3003",
            "title", "INTEGRATION_UNAVAILABLE",
            "status", 503,
            "detail", "The downstream service is temporarily unavailable.",
            "timestamp", Instant.now().toString());
    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
  }
}
