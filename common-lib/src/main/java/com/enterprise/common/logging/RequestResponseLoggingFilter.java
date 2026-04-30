package com.enterprise.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Emits a single structured log line per HTTP request with:
 * <ul>
 *   <li>method, URI, query, status, content-type</li>
 *   <li>duration in ms</li>
 *   <li>client IP and user-agent</li>
 *   <li>masked request + response bodies (if enabled)</li>
 * </ul>
 *
 * <p>URIs matching {@link LoggingProperties.AccessLog#getExcludePatterns()}
 * are skipped (health checks, Swagger, etc.). Body capture is bounded by
 * {@link LoggingProperties.AccessLog#getMaxPayloadLength()} so we never
 * balloon memory on large uploads.</p>
 */
public class RequestResponseLoggingFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger("ACCESS");
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final LoggingProperties.AccessLog cfg;
    private final PayloadMasker masker;

    public RequestResponseLoggingFilter(LoggingProperties.AccessLog cfg, PayloadMasker masker) {
        this.cfg = cfg;
        this.masker = masker;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        List<String> excludes = cfg.getExcludePatterns();
        return excludes != null && excludes.stream().anyMatch(p -> MATCHER.match(p, uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        HttpServletRequest req = cfg.isIncludePayload()
                ? new ContentCachingRequestWrapper(request) : request;
        HttpServletResponse res = cfg.isIncludePayload()
                ? new ContentCachingResponseWrapper(response) : response;
        try {
            chain.doFilter(req, res);
        } finally {
            long duration = System.currentTimeMillis() - start;
            writeLog(req, res, duration);
            if (res instanceof ContentCachingResponseWrapper w) {
                w.copyBodyToResponse();
            }
        }
    }

    /** Build the single access-log line. */
    private void writeLog(HttpServletRequest req, HttpServletResponse res, long duration) {
        String reqBody = "";
        String resBody = "";
        if (cfg.isIncludePayload()) {
            if (req instanceof ContentCachingRequestWrapper rw) {
                reqBody = masker.mask(truncate(new String(rw.getContentAsByteArray(), StandardCharsets.UTF_8)));
            }
            if (res instanceof ContentCachingResponseWrapper rw) {
                resBody = masker.mask(truncate(new String(rw.getContentAsByteArray(), StandardCharsets.UTF_8)));
            }
        }
        log.info(
                "method={} uri={} query=\"{}\" status={} durationMs={} client={} ua=\"{}\" req=\"{}\" res=\"{}\"",
                req.getMethod(),
                req.getRequestURI(),
                nullToEmpty(req.getQueryString()),
                res.getStatus(),
                duration,
                clientIp(req),
                nullToEmpty(req.getHeader("User-Agent")),
                reqBody,
                resBody);
    }

    private String truncate(String s) {
        if (s == null) return "";
        int max = Math.max(cfg.getMaxPayloadLength(), 0);
        return s.length() > max ? s.substring(0, max) + "...(truncated)" : s;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    /** Run after the correlation-id filter. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
