package app.virtual_workspace.exceptions;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import app.virtual_workspace.exceptions.custom.ResourceAlreadyExistsException;
import app.virtual_workspace.exceptions.custom.ResourceNotFoundException;
import app.virtual_workspace.exceptions.custom.TokenRefreshException;
import app.virtual_workspace.shared.dtos.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        // ── 1. Validation Errors (400) ───────────────────────────────────────────

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                List<ErrorResponse.ValidationError> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> ErrorResponse.ValidationError.builder()
                                                .field(error.getField())
                                                .message(error.getDefaultMessage())
                                                .build())
                                .toList();

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(ex.getStatusCode().value())
                                .message("Validation failed for one or more fields")
                                .error("Validation Failed")
                                .errors(errors)
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields",
                                errorResponse);
        }

        // ── 2. Resource Not Found (404) ──────────────────────────────────────────

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleResourceNotFoundException(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.NOT_FOUND, ex.getMessage(), errorResponse);
        }

        // ── 3. Resource Already Exists / Conflict (409) ──────────────────────────

        @ExceptionHandler(ResourceAlreadyExistsException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleResourceAlreadyExistsException(
                        ResourceAlreadyExistsException ex,
                        HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error(HttpStatus.CONFLICT.getReasonPhrase())
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.CONFLICT, ex.getMessage(), errorResponse);
        }

        // ── 4. Database Integrity Violation — duplicate key etc. (409) ───────────

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleDataIntegrityViolationException(
                        DataIntegrityViolationException ex,
                        HttpServletRequest request) {
                log.error("Data integrity violation at [{}]: {}", request.getRequestURI(), ex.getMessage());

                String friendlyMessage = "A record with the provided details already exists. " +
                                "Please check your input and try again.";

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error(HttpStatus.CONFLICT.getReasonPhrase())
                                .message(friendlyMessage)
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.CONFLICT, friendlyMessage, errorResponse);
        }

        // ── 5. Insufficient Authentication (401) ─────────────────────────────────

        @ExceptionHandler(InsufficientAuthenticationException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleInsufficientAuthenticationException(
                        InsufficientAuthenticationException ex,
                        HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                                .message("Full authentication is required to access this resource")
                                .path(request.getRequestURI())
                                .build();

                ApiResponse<ErrorResponse> response = ApiResponse.<ErrorResponse>builder()
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .message("Unauthorized access")
                                .errors(errorResponse)
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // ── 6. Bad Credentials — wrong email / password (401) ────────────────────

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleBadCredentialsException(
                        BadCredentialsException ex,
                        HttpServletRequest request) {
                String message = "The email or password you entered is incorrect. Please double-check and try again.";

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                                .message(message)
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.UNAUTHORIZED, message, errorResponse);
        }

        @ExceptionHandler(TokenRefreshException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleTokenRefreshException(
                        TokenRefreshException ex,
                        HttpServletRequest request) {
                log.warn("Token refresh failure at [{}]: {}", request.getRequestURI(), ex.getMessage());

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.BAD_REQUEST, ex.getMessage(), errorResponse);
        }

        // ── 7. Broader AuthenticationException fallback (401) ────────────────────

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleAuthenticationException(
                        AuthenticationException ex,
                        HttpServletRequest request) {
                log.warn("Authentication failure at [{}]: {}", request.getRequestURI(), ex.getMessage());

                String message = "Authentication failed. Please check your credentials and try again.";

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                                .message(message)
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.UNAUTHORIZED, message, errorResponse);
        }

        // ── 8. JWT — Expired Token (401) ──────────────────────────────────────────

        @ExceptionHandler(ExpiredJwtException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleExpiredJwtException(
                        ExpiredJwtException ex,
                        HttpServletRequest request) {
                log.warn("Expired JWT at [{}]", request.getRequestURI());

                String message = "Your session has expired. Please log in again.";

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                                .message(message)
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.UNAUTHORIZED, message, errorResponse);
        }

        // ── 9. JWT — Invalid / Malformed / Signature mismatch (401) ─────────────

        @ExceptionHandler(JwtException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleJwtException(
                        JwtException ex,
                        HttpServletRequest request) {
                log.warn("Invalid JWT at [{}]: {}", request.getRequestURI(), ex.getMessage());

                String message = "Invalid or malformed authentication token. Please log in again.";

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                                .message(message)
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.UNAUTHORIZED, message, errorResponse);
        }

        // ── 10. Unhandled Runtime Exceptions (500) ───────────────────────────────

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleRuntimeException(
                        RuntimeException ex,
                        HttpServletRequest request) {
                log.error("Unhandled runtime exception at [{}]", request.getRequestURI(), ex);

                String message = "Oops! Something went wrong on our end. Please try again in a few minutes.";

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                .message(message)
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, errorResponse);
        }

        // ── 7. Broadest Catch-All — Checked Exceptions (500) ────────────────────

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {
                log.error("Unexpected exception at [{}]", request.getRequestURI(), ex);

                String message = "Oops! Something went wrong on our end. Please try again in a few minutes.";

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                .message(message)
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, errorResponse);
        }

        // ── 8. Access Denied — Checked Exceptions (403) ────────────────────

        @ExceptionHandler(AuthorizationDeniedException.class)
        public ResponseEntity<ApiResponse<ErrorResponse>> handleAuthorizationDeniedException(
                        AuthorizationDeniedException ex,
                        HttpServletRequest request) {
                log.warn("Access denied at [{}]: {}", request.getRequestURI(), ex.getMessage());

                String message = "You do not have permission to perform this action.";

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timeStamp(LocalDateTime.now())
                                .status(HttpStatus.FORBIDDEN.value())
                                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                                .message(message)
                                .path(request.getRequestURI())
                                .build();

                return buildResponseEntity(HttpStatus.FORBIDDEN, message, errorResponse);
        }

        // ── Shared builder ───────────────────────────────────────────────────────

        private ResponseEntity<ApiResponse<ErrorResponse>> buildResponseEntity(
                        HttpStatus status,
                        String message,
                        ErrorResponse errorResponse) {
                ApiResponse<ErrorResponse> response = ApiResponse.<ErrorResponse>builder()
                                .status(status.value())
                                .message(message)
                                .errors(errorResponse)
                                .build();

                return ResponseEntity.status(status).body(response);
        }

}