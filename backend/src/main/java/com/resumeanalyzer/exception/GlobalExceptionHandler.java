package com.resumeanalyzer.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception-handling foundation.
 *
 * Phase 0 established a minimal, generic fallback handler. Phase 1 added
 * the authentication-related cases for {@code /api/auth/register} and
 * {@code /api/auth/login}. Phase 2 adds handling for resume upload/parsing
 * problems introduced by {@code /api/resumes}. Phase 6 adds handling for
 * screening-batch request validation and Recruiter-only access control
 * introduced by {@code /api/screening-batches}. The original generic
 * fallback is preserved for anything unanticipated.
 *
 * All responses use the same consistent JSON shape and never expose stack
 * traces, SQL details, internal class names, or security-sensitive
 * information (e.g. why a login attempt actually failed).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * A registration attempt for an email address that already exists.
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Invalid login credentials (wrong password or unknown email). A single,
     * generic message is used for both cases so a caller cannot infer
     * whether a given email is registered.
     */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationServiceException.class})
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailure(Exception ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    /**
     * An uploaded resume that is empty, has an unsupported extension, is
     * corrupted, or has no extractable text.
     */
    @ExceptionHandler(ResumeProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleResumeProcessing(ResumeProcessingException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * An uploaded file exceeding the configured multipart size limit.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "The uploaded file exceeds the maximum allowed size");
    }

    /**
     * A referenced resume/job-description that does not exist or is not
     * owned by the authenticated caller. Both cases return the same 404
     * response so ownership of another user's data cannot be inferred.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * A screening-batch create request that is invalid at the request
     * level (blank/oversized name, zero files, or too many files) -
     * distinct from a single file within it being unusable, which is
     * reported per-file in the 201 response body instead (see
     * {@code ScreeningBatchService}).
     */
    @ExceptionHandler(ScreeningBatchValidationException.class)
    public ResponseEntity<Map<String, Object>> handleScreeningBatchValidation(ScreeningBatchValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * A required multipart FILE part (e.g. "files") missing entirely from
     * the request. Spring raises this - not
     * {@link MissingServletRequestParameterException} - specifically for
     * {@code MultipartFile}/{@code Part}-typed {@code @RequestParam}s, so
     * it needs its own handler alongside the generic one below.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestPart(MissingServletRequestPartException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Required request part '" + ex.getRequestPartName() + "' is missing");
    }

    /**
     * A required multipart field/part (e.g. the batch "name", or "files")
     * missing entirely from the request - distinct from
     * {@link MethodArgumentNotValidException}, which only applies to
     * {@code @Valid @RequestBody} DTOs, not multipart request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Required request parameter '" + ex.getParameterName() + "' is missing");
    }

    /**
     * A caller who is authenticated but not permitted to perform the
     * requested action - e.g. a Job Seeker attempting to create or read a
     * screening batch, which is Recruiter-only. Distinct from
     * {@link ResourceNotFoundException}: this is a blanket role
     * restriction that applies regardless of which specific resource id
     * was requested, so there is no existence-leak concern in returning
     * 403 here (contrast with cross-user ownership, which returns 404 -
     * see {@code ScreeningBatchService#resolveOwnedBatch}).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Bean Validation failures on request DTOs (e.g. malformed email,
     * missing field, password too short). Returns a field-level breakdown
     * so the client can correct the request, without leaking anything
     * about internal implementation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", "Validation failed");
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * A path variable or request parameter that could not be converted to
     * its declared type - e.g. a non-numeric {@code {id}} such as
     * GET /api/analysis/abc. This is a malformed-request problem (400),
     * not an unanticipated server failure, and the response never
     * includes the exception's own message (which would otherwise name
     * the parameter/type involved) - only a fixed, generic message.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "The request contains a malformed parameter");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception ex) {
        // The real exception (type, message, stack trace) is logged
        // server-side only. The response body never includes ex.getMessage()
        // directly: for an *unanticipated* exception there is no guarantee
        // its message is free of stack traces, SQL/constraint details,
        // internal class names, or other implementation detail - exactly
        // what this class's own contract (see class javadoc) promises
        // never to expose to a caller.
        log.error("Unhandled exception while processing request", ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        body.put("message", "An unexpected error occurred");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }
}
