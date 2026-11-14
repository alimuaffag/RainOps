package com.rainops.common.exception;

import com.rainops.incident.exception.IncidentNotFoundException;
import com.rainops.incident.exception.InvalidStatusTransitionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(IncidentNotFoundException.class)
    public ResponseEntity<Object> notFound(IncidentNotFoundException ex, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Object> invalidTransition(InvalidStatusTransitionException ex, WebRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<Object> conflict(Exception ex, WebRequest request) {
        return error(HttpStatus.CONFLICT, "The change could not be saved. Reload the incident and try again.", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(status, "Request validation failed.", request);
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return new ResponseEntity<>(problem, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(status, "Request parameters are invalid.", request);
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> result.getResolvableErrors().forEach(
                e -> errors.putIfAbsent(
                    result.getMethodParameter().getParameterName() == null ? "parameter" :
                        result.getMethodParameter().getParameterName(), e.getDefaultMessage())));
        problem.setProperty("errors", errors);
        return new ResponseEntity<>(problem, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return new ResponseEntity<>(problem(status,
                "Invalid JSON. Check field names, data types, and enum values.", request), headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail detail = body instanceof ProblemDetail p ? p :
                problem(status, status.is4xxClientError() ? "The request could not be processed." :
                        "An unexpected server error occurred.", request);
        return super.handleExceptionInternal(ex, detail, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception ex, WebRequest request) {
        log.error("Unhandled API error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.", request);
    }

    private ResponseEntity<Object> error(HttpStatus status, String detail, WebRequest request) {
        return ResponseEntity.status(status).body(problem(status, detail, request));
    }

    private ProblemDetail problem(HttpStatusCode status, String detail, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(request.getDescription(false).replaceFirst("^uri=", "")));
        return problem;
    }
}
