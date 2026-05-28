package com.ailending.app.controller;

import com.ailending.common.exception.BaseException;
import com.ailending.lending.exception.InvalidStatusTransitionException;
import com.ailending.lending.exception.LoanNotFoundException;
import com.ailending.workflow.exception.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates domain exceptions into appropriate HTTP status codes and structured
 * error response bodies.
 *
 * <p>Error body shape:
 * <pre>
 * {
 *   "timestamp": "2026-05-27T09:41:00Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Loan not found: loan-xyz"
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -----------------------------------------------------------------------
    // Loan domain errors
    // -----------------------------------------------------------------------

    /** HTTP 404 — loan not found by ID. */
    @ExceptionHandler(LoanNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleLoanNotFound(LoanNotFoundException ex) {
        log.warn("Loan not found: {}", ex.getMessage());
        return errorBody(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** HTTP 400 — invalid FR-8 workflow transition. */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidTransition(InvalidStatusTransitionException ex) {
        log.warn("Invalid status transition: {}", ex.getMessage());
        return errorBody(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // AI / infrastructure errors
    // -----------------------------------------------------------------------

    /**
     * HTTP 503 — LLM provider is offline.
     * Thrown by {@code RagEngine} when {@code llmProvider.isAvailable()} returns {@code false}.
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> handleIllegalState(IllegalStateException ex) {
        log.error("Service unavailable: {}", ex.getMessage());
        return errorBody(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    /** HTTP 422 — validation error (bad request arguments, blank fields, etc.). */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return errorBody(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    /** HTTP 400 — workflow operation rejected (bad transition, unknown loan, etc.). */
    @ExceptionHandler(WorkflowException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleWorkflowException(WorkflowException ex) {
        log.warn("Workflow error: {}", ex.getMessage());
        return errorBody(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** HTTP 422 — catch-all for platform domain validation exceptions (AuditException, PolicyException, etc.). */
    @ExceptionHandler(BaseException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, Object> handleBaseException(BaseException ex) {
        log.warn("Domain validation error: {}", ex.getMessage());
        return errorBody(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // Private helper
    // -----------------------------------------------------------------------

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("timestamp", Instant.now().toString());
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        return body;
    }
}
