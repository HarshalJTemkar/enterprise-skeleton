package com.enterprise.sample.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

/**
 * Slice test for the GraphQL resolver. Confirms:
 *
 * <ul>
 *   <li>{@code whoAmI} returns the {@code QueryResponse}-shaped envelope.
 *   <li>Validation failure produces an error with stable {@code errorCode} extension via the
 *       platform's {@code GraphQlExceptionResolver}.
 * </ul>
 */
@GraphQlTest(SampleGraphQlController.class)
@Import(SampleGraphQlControllerTest.SchemaSource.class)
class SampleGraphQlControllerTest {

  @Autowired GraphQlTester tester;

  @Test
  void whoAmI_returns_envelope() {
    tester
        .document(
            """
                query { whoAmI(subject: "alice", roles: "[ROLE_USER]") {
                    status
                    data { subject roles }
                } }
                """)
        .execute()
        .path("whoAmI.status")
        .entity(String.class)
        .isEqualTo("success")
        .path("whoAmI.data.subject")
        .entity(String.class)
        .isEqualTo("alice")
        .path("whoAmI.data.roles")
        .entity(String.class)
        .isEqualTo("[ROLE_USER]");
  }

  @Test
  void create_order_with_invalid_quantity_returns_business_rule_error_code() {
    tester
        .document(
            """
                mutation { createOrder(
                    input: { product: "widget", quantity: 0 },
                    subject: "alice"
                ) { data { orderId } } }
                """)
        .execute()
        .errors()
        .satisfy(
            errs -> {
              org.assertj.core.api.Assertions.assertThat(errs).isNotEmpty();
              var ext = errs.get(0).getExtensions();
              org.assertj.core.api.Assertions.assertThat(ext)
                  .containsEntry("errorCode", "ERR-2001")
                  .containsEntry("classification", "BUSINESS_RULE_VIOLATION");
            });
  }

  /**
   * Minimal {@code MessageService} stub so the GraphQL exception resolver can resolve i18n keys
   * inside the slice test.
   */
  @TestConfiguration
  static class SchemaSource {
    @Bean
    com.enterprise.common.i18n.MessageService messageService() {
      com.enterprise.common.i18n.MessageService mock =
          org.mockito.Mockito.mock(com.enterprise.common.i18n.MessageService.class);
      org.mockito.Mockito.when(mock.get(org.mockito.Mockito.anyString()))
          .thenAnswer(i -> "msg:" + i.getArgument(0));
      org.mockito.Mockito.when(
              mock.getOrDefault(
                  org.mockito.Mockito.anyString(),
                  org.mockito.Mockito.anyString(),
                  org.mockito.Mockito.any(Object[].class)))
          .thenAnswer(i -> i.getArgument(1));
      return mock;
    }

    @Bean
    com.enterprise.common.graphql.GraphQlProperties graphQlProperties() {
      return new com.enterprise.common.graphql.GraphQlProperties();
    }

    @Bean
    com.enterprise.common.graphql.GraphQlExceptionResolver graphQlExceptionResolver(
        com.enterprise.common.i18n.MessageService messages,
        com.enterprise.common.graphql.GraphQlProperties props) {
      return new com.enterprise.common.graphql.GraphQlExceptionResolver(messages, props);
    }
  }
}
