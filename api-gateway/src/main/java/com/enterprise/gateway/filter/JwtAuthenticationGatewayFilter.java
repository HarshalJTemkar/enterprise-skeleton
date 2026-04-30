package com.enterprise.gateway.filter;

import com.enterprise.common.config.CommonProperties;
import com.enterprise.gateway.config.GatewayJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Validates the JWT on every request unless the path is in gateway.security.jwt.public-paths.
 * Extracted claims (sub, roles) are forwarded as headers to downstream services.
 */
@Slf4j
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

  private static final AntPathMatcher MATCHER = new AntPathMatcher();
  private final CommonProperties commonProps;
  private final GatewayJwtProperties jwtProps;
  private final SecretKey key;

  public JwtAuthenticationGatewayFilter(
      CommonProperties commonProps, GatewayJwtProperties jwtProps) {
    this.commonProps = commonProps;
    this.jwtProps = jwtProps;
    this.key = Keys.hmacShaKeyFor(commonProps.jwt().secret().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getURI().getPath();

    if (isPublic(path)) {
      return chain.filter(exchange);
    }

    String header = request.getHeaders().getFirst(commonProps.jwt().headerName());
    if (header == null || !header.startsWith(commonProps.jwt().tokenPrefix())) {
      return unauthorized(exchange, "Missing or malformed Authorization header");
    }

    String token = header.substring(commonProps.jwt().tokenPrefix().length());
    try {
      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      ServerHttpRequest mutated =
          request
              .mutate()
              .header("X-Auth-Subject", claims.getSubject())
              .header("X-Auth-Roles", String.valueOf(claims.get("roles", Object.class)))
              .build();
      return chain.filter(exchange.mutate().request(mutated).build());
    } catch (Exception ex) {
      log.debug("JWT validation failed: {}", ex.getMessage());
      return unauthorized(exchange, "Invalid or expired token");
    }
  }

  private boolean isPublic(String path) {
    return jwtProps.publicPaths().stream().anyMatch(p -> MATCHER.match(p, path));
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
    ServerHttpResponse resp = exchange.getResponse();
    resp.setStatusCode(HttpStatus.UNAUTHORIZED);
    resp.getHeaders().add("X-Auth-Error", msg);
    return resp.setComplete();
  }

  @Override
  public int getOrder() {
    return -100;
  }
}
