package com.enterprise.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterprise.common.api.ErrorResponse;
import com.enterprise.common.i18n.MessageService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest req;

    @BeforeEach
    void setUp() {
        MessageService ms = Mockito.mock(MessageService.class);
        Mockito.when(ms.get(Mockito.anyString())).thenAnswer(inv -> "msg:" + inv.getArgument(0));
        Mockito.when(ms.getOrDefault(Mockito.anyString(), Mockito.anyString(), Mockito.any(Object[].class)))
                .thenAnswer(inv -> inv.getArgument(1));
        handler = new GlobalExceptionHandler(ms);
        req = new MockHttpServletRequest("POST", "/api/test");
    }

    private static ErrorResponse envelope(ResponseEntity<ProblemDetail> r) {
        return (ErrorResponse) r.getBody().getProperties().get("error");
    }

    // ---- Custom hierarchy ------------------------------------------------

    @Test
    void base_exception_uses_error_code_status() {
        var ex = new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "exists");
        var r = handler.handleBase(ex, req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-2004");
    }

    @Test
    void duplicate_resource_exception_carries_details() {
        var ex = new DuplicateResourceException("User", "username", "alice");
        var r = handler.handleBase(ex, req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(envelope(r).details()).containsEntry("field", "username");
    }

    // ---- Validation ------------------------------------------------------

    @Test
    void constraint_violation_returns_400_with_field_errors() {
        ConstraintViolation<?> cv = Mockito.mock(ConstraintViolation.class);
        jakarta.validation.Path p = Mockito.mock(jakarta.validation.Path.class);
        Mockito.when(p.toString()).thenReturn("user.email");
        Mockito.when(cv.getPropertyPath()).thenReturn(p);
        Mockito.when(cv.getInvalidValue()).thenReturn("not-an-email");
        Mockito.when(cv.getMessage()).thenReturn("must be email");
        var r = handler.handleConstraint(new ConstraintViolationException(Set.of(cv)), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-0002");
        assertThat(envelope(r).errors()).hasSize(1);
    }

    // ---- Request parsing -------------------------------------------------

    @Test
    void missing_param_returns_400_missing_parameter_code() {
        var ex = new MissingServletRequestParameterException("page", "int");
        var r = handler.handleMissingParam(ex, req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-0010");
        assertThat(envelope(r).details()).containsEntry("parameter", "page");
    }

    @Test
    void missing_header_returns_400_missing_header_code() throws Exception {
        var ex = new MissingRequestHeaderException("X-Tenant",
                new org.springframework.core.MethodParameter(
                        Object.class.getDeclaredMethod("toString"), -1));
        var r = handler.handleMissingHeader(ex, req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-0011");
    }

    // ---- HTTP-level ------------------------------------------------------

    @Test
    void method_not_allowed_returns_405() {
        var r = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("DELETE"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void unsupported_media_type_returns_415() {
        var r = handler.handleUnsupportedMediaType(
                new HttpMediaTypeNotSupportedException("text/xml"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void not_acceptable_returns_406() {
        var r = handler.handleNotAcceptable(
                new HttpMediaTypeNotAcceptableException("not acceptable"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
    }

    @Test
    void max_upload_size_returns_413() {
        var r = handler.handleMaxUpload(new MaxUploadSizeExceededException(10 * 1024 * 1024), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(envelope(r).details()).containsKey("maxBytes");
    }

    @Test
    void response_status_exception_is_honoured() {
        var r = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.GONE, "long gone"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    // ---- Spring Security ------------------------------------------------

    @Test
    void bad_credentials_returns_401_invalid_credentials() {
        var r = handler.handleBadCredentials(new BadCredentialsException("nope"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-1003");
    }

    @Test
    void locked_returns_423_account_locked() {
        var r = handler.handleLocked(new LockedException("locked"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-1007");
    }

    @Test
    void disabled_returns_403_account_disabled() {
        var r = handler.handleDisabled(new DisabledException("off"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-1008");
    }

    @Test
    void access_denied_returns_403() {
        var r = handler.handleAccessDenied(new AccessDeniedException("nope"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-1006");
    }

    // ---- Persistence ----------------------------------------------------

    @Test
    void duplicate_key_returns_409_duplicate_resource() {
        var r = handler.handleIntegrity(new DuplicateKeyException("dup"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-2004");
    }

    @Test
    void data_integrity_violation_returns_409_data_integrity() {
        var r = handler.handleIntegrity(
                new DataIntegrityViolationException("violates constraint \"uk_users_email\""), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-5004");
        assertThat(envelope(r).details()).containsEntry("constraint", "uk_users_email");
    }

    @Test
    void optimistic_lock_returns_409() {
        var r = handler.handleOptimisticLock(
                new OptimisticLockingFailureException("stale"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-2005");
    }

    @Test
    void cannot_acquire_lock_returns_423() {
        var r = handler.handleLock(new CannotAcquireLockException("busy"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void empty_result_returns_404() {
        var r = handler.handleEmptyResult(new EmptyResultDataAccessException(1), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void query_timeout_returns_504() {
        var r = handler.handleQueryTimeout(new QueryTimeoutException("slow"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-5003");
    }

    @Test
    void db_unavailable_returns_503() {
        var r = handler.handleDbDown(new DataAccessResourceFailureException("down"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-5002");
    }

    // ---- JVM-level ------------------------------------------------------

    @Test
    void illegal_argument_returns_400() {
        var r = handler.handleIllegalArg(new IllegalArgumentException("bad"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unsupported_operation_returns_501() {
        var r = handler.handleUnsupported(new UnsupportedOperationException("not yet"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    void timeout_returns_504() {
        var r = handler.handleTimeout(new TimeoutException("slow"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    // ---- Catch-all ------------------------------------------------------

    @Test
    void unknown_exception_returns_500() {
        var r = handler.handleUnknown(new RuntimeException("boom"), req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(envelope(r).errorCode()).isEqualTo("ERR-0001");
    }
}
