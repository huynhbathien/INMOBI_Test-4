package com.mycompany.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.dto.APIResponse;
import com.mycompany.enums.EnumAuthError;
import com.mycompany.enums.EnumError;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        // Authentication & Authorization errors
        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<APIResponse<Object>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
                log.warn("User not found: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(APIResponse.error(
                                                EnumAuthError.USER_NOT_FOUND.getCode(),
                                                EnumAuthError.USER_NOT_FOUND.getMessage(),
                                                ex.getMessage()));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<APIResponse<Object>> handleBadCredentialsException(BadCredentialsException ex) {
                log.warn("Invalid credentials: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(APIResponse.error(
                                                EnumAuthError.INVALID_CREDENTIALS.getCode(),
                                                EnumAuthError.INVALID_CREDENTIALS.getMessage(),
                                                "Username or password is incorrect"));
        }

        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<APIResponse<Object>> handleDisabledException(DisabledException ex) {
                log.warn("Account disabled: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(APIResponse.error(
                                                EnumAuthError.ACCOUNT_DISABLED.getCode(),
                                                EnumAuthError.ACCOUNT_DISABLED.getMessage(),
                                                ex.getMessage()));
        }

        // Validation errors
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<APIResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
                String message = ex.getBindingResult().getFieldError() != null
                                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                                : "Validation failed";
                log.warn("Validation error: {}", message);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(APIResponse.error(
                                                400,
                                                "Validation failed",
                                                message));
        }

        // General exceptions
        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<APIResponse<Object>> handleRuntimeException(RuntimeException ex) {
                log.error("Runtime exception: ", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(APIResponse.error(
                                                EnumAuthError.INTERNAL_ERROR.getCode(),
                                                EnumAuthError.INTERNAL_ERROR.getMessage(),
                                                ex.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<APIResponse<Object>> handleException(Exception ex) {
                log.error("Unexpected exception: ", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(APIResponse.error(
                                                EnumAuthError.INTERNAL_ERROR.getCode(),
                                                EnumAuthError.INTERNAL_ERROR.getMessage(),
                                                "An unexpected error occurred"));
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<APIResponse<Object>> handleResponseStatusException(ResponseStatusException ex) {
                log.error("ResponseStatusException", ex);
                HttpStatusCode status = ex.getStatusCode();
                return ResponseEntity.status(status)
                                .body(APIResponse.error(
                                                status.value(),
                                                status.toString(),
                                                ex.getReason()));
        }

        @ExceptionHandler(AppException.class)
        public ResponseEntity<APIResponse<Object>> handleAppException(AppException ex) {
                log.warn("AppException: code={}, message={}", ex.getCode(), ex.getMessage());
                HttpStatus status = resolveHttpStatus(ex.getCode());
                return ResponseEntity.status(status)
                                .body(APIResponse.error(
                                                ex.getCode(),
                                                ex.getMessage(),
                                                null));
        }

        /**
         * Maps application error codes to semantically correct HTTP status codes.
         * NOT_FOUND (404): resource does not exist.
         * CONFLICT (409): resource already in the requested state.
         * Default (400): bad request / invalid input.
         */
        private HttpStatus resolveHttpStatus(int code) {
                // 404 — resource not found
                if (code == EnumError.USER_NOT_FOUND.getCode()
                                || code == EnumAuthError.USER_NOT_FOUND.getCode()) {
                        return HttpStatus.NOT_FOUND;
                }
                return HttpStatus.BAD_REQUEST;
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<APIResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
                log.warn("IllegalArgumentException: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(APIResponse.error(
                                                400,
                                                "Bad request",
                                                ex.getMessage()));
        }
}
