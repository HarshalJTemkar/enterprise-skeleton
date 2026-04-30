package com.enterprise.common.web;

import com.enterprise.common.config.CommonProperties;
import java.util.Arrays;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Registers a {@link CorsFilter} driven by
 * {@link com.enterprise.common.config.CommonProperties.Web.Cors}.
 *
 * <p>Active only when:</p>
 * <ul>
 *   <li>{@code enterprise.common.web.cors.enabled=true}</li>
 *   <li>The servlet stack is on the classpath</li>
 * </ul>
 *
 * <p>{@code allowedOrigins / allowedMethods / allowedHeaders} are
 * comma-separated lists; pass {@code *} to allow all (the filter automatically
 * switches to {@code allowedOriginPatterns} so credentials still work).</p>
 */
@Configuration
@ConditionalOnClass(CorsFilter.class)
@ConditionalOnProperty(prefix = "enterprise.common.web.cors", name = "enabled",
        havingValue = "true", matchIfMissing = false)
public class CorsAutoConfiguration {

    /** The CORS filter bean. */
    @Bean
    @ConditionalOnMissingBean
    public CorsFilter corsFilter(CommonProperties props) {
        var cors = props.web().cors();
        CorsConfiguration cfg = new CorsConfiguration();

        // "*" + credentials is illegal under the CORS spec; switch to patterns.
        if ("*".equals(cors.allowedOrigins())) {
            cfg.addAllowedOriginPattern("*");
        } else {
            cfg.setAllowedOrigins(splitCsv(cors.allowedOrigins()));
        }
        cfg.setAllowedMethods(splitCsvOrAny(cors.allowedMethods()));
        cfg.setAllowedHeaders(splitCsvOrAny(cors.allowedHeaders()));
        cfg.setExposedHeaders(java.util.List.of(
                "X-Correlation-ID", "X-Request-ID", "X-Trace-ID"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(source);
    }

    private static java.util.List<String> splitCsv(String csv) {
        return csv == null ? java.util.List.of()
                : Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Treat {@code "*"} as "any". */
    private static java.util.List<String> splitCsvOrAny(String csv) {
        if (csv == null || csv.isBlank() || "*".equals(csv.trim())) return java.util.List.of("*");
        return splitCsv(csv);
    }
}
