package com.fourguard.wms.presentation.advice;

import com.fourguard.wms.domain.exception.AccountPermanentlyLockedException;
import com.fourguard.wms.domain.exception.AccountTemporarilyLockedException;
import com.fourguard.wms.domain.exception.ConflictException;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.InvalidCredentialsException;
import com.fourguard.wms.domain.exception.InvalidFsmTransitionException;
import com.fourguard.wms.domain.exception.TokenExpiredException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.shared.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Domain-specific exception handler extensions.
 * Registers handlers for clean domain model exceptions.
 * Has higher priority than the generic exceptions mapping in GlobalExceptionHandler.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class DomainExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("[AUTH-ERROR] Credentials verification failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenExpired(TokenExpiredException ex) {
        log.warn("[AUTH-ERROR] Security token expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
        log.warn("[NOT-FOUND] Requested entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(ValidationException ex) {
        log.warn("[VALIDATION-ERROR] Domain validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── FSM Transition ────────────────────────────────────────────────────────

    /**
     * HTTP 422 — the requested FSM state transition is not permitted by business rules.
     * E.g. BLOCKED → MAINTENANCE or INACTIVE → BLOCKED.
     */
    @ExceptionHandler(InvalidFsmTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidFsmTransition(InvalidFsmTransitionException ex) {
        log.warn("[FSM-ERROR] Invalid FSM transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Business Conflict ─────────────────────────────────────────────────────

    /**
     * HTTP 409 — a business conflict prevents the operation.
     * E.g. duplicate location code, or location has active inventory.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        log.warn("[CONFLICT] Business conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Lockout ───────────────────────────────────────────────────────────────

    /**
     * HTTP 423 — account temporarily locked due to repeated failed login attempts.
     * Response includes the number of minutes remaining in the lock window.
     */
    @ExceptionHandler(AccountTemporarilyLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleTempLocked(AccountTemporarilyLockedException ex) {
        log.warn("[LOCKOUT] Rejected — account temporarily locked ({} min remaining): {}",
                 ex.getMinutesRemaining(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * HTTP 423 — account permanently locked; admin intervention required.
     */
    @ExceptionHandler(AccountPermanentlyLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePermLocked(AccountPermanentlyLockedException ex) {
        log.warn("[LOCKOUT] Rejected — account permanently locked: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiResponse.error(ex.getMessage()));
    }
}
