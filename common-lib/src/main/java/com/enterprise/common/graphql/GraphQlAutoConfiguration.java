package com.enterprise.common.graphql;

import com.enterprise.common.i18n.MessageService;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.scalars.ExtendedScalars;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Wires GraphQL features for any service that adds {@code spring-boot-starter-graphql}:
 *
 * <ul>
 *   <li>Custom scalars: {@code DateTime}, {@code Date}, {@code Time}, {@code UUID}, {@code JSON},
 *       {@code Long}, {@code BigDecimal}, {@code Url}, {@code Object}.
 *   <li>{@code MaxQueryDepthInstrumentation} and {@code MaxQueryComplexityInstrumentation}
 *       (configurable thresholds).
 *   <li>{@link GraphQlExceptionResolver} so platform exceptions emit structured GraphQL errors with
 *       the same {@code ErrorCode} as REST.
 * </ul>
 *
 * <p>Disabled with {@code enterprise.common.graphql.enabled=false}. Skipped automatically if Spring
 * GraphQL is not on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(
    name = {
      "org.springframework.graphql.execution.RuntimeWiringConfigurer",
      "graphql.scalars.ExtendedScalars"
    })
@ConditionalOnProperty(
    prefix = "enterprise.common.graphql",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(GraphQlProperties.class)
public class GraphQlAutoConfiguration {

  /** Registers the platform's custom scalars on the runtime wiring. */
  @Bean
  @ConditionalOnProperty(
      prefix = "enterprise.common.graphql.scalars",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public RuntimeWiringConfigurer enterpriseScalarsRuntimeWiringConfigurer() {
    return wiring ->
        wiring
            .scalar(ExtendedScalars.DateTime)
            .scalar(ExtendedScalars.Date)
            .scalar(ExtendedScalars.Time)
            .scalar(ExtendedScalars.UUID)
            .scalar(ExtendedScalars.Json)
            .scalar(ExtendedScalars.GraphQLLong)
            .scalar(ExtendedScalars.GraphQLBigDecimal)
            .scalar(ExtendedScalars.Url)
            .scalar(ExtendedScalars.Object);
  }

  /** Bounds query depth to deflect malicious / runaway queries. */
  @Bean
  public Instrumentation enterpriseMaxQueryDepth(GraphQlProperties props) {
    int depth = props.getMaxQueryDepth() <= 0 ? Integer.MAX_VALUE : props.getMaxQueryDepth();
    return new MaxQueryDepthInstrumentation(depth);
  }

  /** Bounds query complexity (each field counts as 1 by default). */
  @Bean
  public Instrumentation enterpriseMaxQueryComplexity(GraphQlProperties props) {
    int complexity =
        props.getMaxQueryComplexity() <= 0 ? Integer.MAX_VALUE : props.getMaxQueryComplexity();
    return new MaxQueryComplexityInstrumentation(complexity);
  }

  /** Maps platform exceptions into GraphQL errors with stable error codes. */
  @Bean
  @ConditionalOnProperty(
      prefix = "enterprise.common.graphql.error-mapping",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public GraphQlExceptionResolver enterpriseGraphQlExceptionResolver(
      MessageService messages, GraphQlProperties props) {
    return new GraphQlExceptionResolver(messages, props);
  }
}
