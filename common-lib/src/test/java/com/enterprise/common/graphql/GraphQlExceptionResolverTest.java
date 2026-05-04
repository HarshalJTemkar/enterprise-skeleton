package com.enterprise.common.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.enterprise.common.exception.BusinessException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.i18n.MessageService;
import graphql.GraphQLError;
import graphql.execution.ResultPath;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

class GraphQlExceptionResolverTest {

  private GraphQlExceptionResolver resolver;
  private DataFetchingEnvironment env;

  @BeforeEach
  void setUp() {
    MessageService messages = Mockito.mock(MessageService.class);
    when(messages.get(Mockito.anyString())).thenAnswer(inv -> "msg:" + inv.getArgument(0));
    when(messages.getOrDefault(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(Object[].class)))
        .thenAnswer(inv -> inv.getArgument(1));
    resolver = new GraphQlExceptionResolver(messages, new GraphQlProperties());

    env =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .executionStepInfo(
                graphql.execution.ExecutionStepInfo.newExecutionStepInfo()
                    .path(ResultPath.parse("/whoAmI"))
                    .type(graphql.Scalars.GraphQLString)
                    .build())
            .build();
  }

  private GraphQLError resolve(Throwable t) {
    return resolver.resolveToSingleError(t, env);
  }

  @Test
  void business_exception_carries_error_code_in_extensions() {
    GraphQLError e = resolve(new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "exists"));
    assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    assertThat(e.getExtensions()).containsEntry("errorCode", "ERR-2004");
    assertThat(e.getExtensions()).containsEntry("classification", "DUPLICATE_RESOURCE");
  }

  @Test
  void bad_credentials_maps_to_unauthorized_with_invalid_credentials_code() {
    GraphQLError e = resolve(new BadCredentialsException("nope"));
    assertThat(e.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
    assertThat(e.getExtensions()).containsEntry("errorCode", "ERR-1003");
  }

  @Test
  void locked_account_maps_to_unauthorized_with_account_locked_code() {
    GraphQLError e = resolve(new LockedException("locked"));
    assertThat(e.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
    assertThat(e.getExtensions()).containsEntry("errorCode", "ERR-1007");
  }

  @Test
  void access_denied_maps_to_forbidden() {
    GraphQLError e = resolve(new AccessDeniedException("denied"));
    assertThat(e.getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
    assertThat(e.getExtensions()).containsEntry("errorCode", "ERR-1006");
  }

  @Test
  void expired_jwt_maps_to_token_expired() {
    ExpiredJwtException ex = Mockito.mock(ExpiredJwtException.class);
    GraphQLError e = resolve(ex);
    assertThat(e.getExtensions()).containsEntry("errorCode", "ERR-1004");
  }

  @Test
  void malformed_jwt_maps_to_token_invalid() {
    GraphQLError e = resolve(new MalformedJwtException("bad"));
    assertThat(e.getExtensions()).containsEntry("errorCode", "ERR-1005");
  }

  @Test
  void duplicate_key_maps_to_duplicate_resource() {
    GraphQLError e = resolve(new DuplicateKeyException("dup"));
    assertThat(e.getExtensions()).containsEntry("errorCode", "ERR-2004");
  }

  @Test
  void integrity_violation_maps_to_data_integrity() {
    GraphQLError e = resolve(new DataIntegrityViolationException("fk"));
    assertThat(e.getExtensions()).containsEntry("errorCode", "ERR-5004");
  }

  @Test
  void optimistic_lock_maps_to_conflict() {
    GraphQLError e = resolve(new OptimisticLockingFailureException("stale"));
    assertThat(e.getExtensions()).containsEntry("errorCode", "ERR-2005");
  }

  @Test
  void illegal_argument_maps_to_bad_request() {
    GraphQLError e = resolve(new IllegalArgumentException("bad"));
    assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    assertThat(e.getExtensions()).containsEntry("errorCode", "ERR-0003");
  }

  @Test
  void unknown_exception_maps_to_internal_error_without_leaking_message() {
    GraphQlProperties strict = new GraphQlProperties();
    strict.getErrorMapping().setExposeMessage(false);
    var strictResolver =
        new GraphQlExceptionResolver(
            Mockito.mock(MessageService.class, Mockito.RETURNS_DEEP_STUBS), strict);
    GraphQLError e = strictResolver.resolveToSingleError(new RuntimeException("secret"), env);
    assertThat(e.getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR);
    assertThat(e.getExtensions()).containsEntry("errorCode", "ERR-0001");
    assertThat(e.getMessage()).doesNotContain("secret");
  }

  @Test
  void timestamp_and_classification_always_present() {
    GraphQLError e = resolve(new BusinessException(ErrorCode.NOT_FOUND, "missing"));
    assertThat(e.getExtensions()).containsKey("timestamp");
    assertThat(e.getExtensions()).containsKey("classification");
  }

  @Test
  void source_locations_default_to_empty_list_when_no_field_path() {
    GraphQLError e = resolve(new BusinessException(ErrorCode.BAD_REQUEST, "x"));
    // GraphqlErrorBuilder always returns at least an empty list
    List<SourceLocation> locs = e.getLocations();
    assertThat(locs).isNotNull();
  }
}
