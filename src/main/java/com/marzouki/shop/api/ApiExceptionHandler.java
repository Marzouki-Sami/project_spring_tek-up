package com.marzouki.shop.api;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Reports API errors as RFC 9457 problem documents. */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    @ExceptionHandler(com.marzouki.shop.common.TooManyRequestsException.class)
    ProblemDetail tooMany(com.marzouki.shop.common.TooManyRequestsException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Too many requests", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ProblemDetail integrity() {
        return problem(HttpStatus.CONFLICT, "Conflict", "That change conflicts with another one that just happened. Reload and try again.");
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    ProblemDetail wrongType(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad request", "The value of '" + ex.getName() + "' is not valid");
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    ProblemDetail missingParameter(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad request", "The parameter '" + ex.getParameterName() + "' is required");
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ProblemDetail wrongMethod() {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", "That method is not supported here");
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler({BadCredentialsException.class})
    ProblemDetail badCredentials() {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid credentials");
    }

    @ExceptionHandler({LockedException.class, DisabledException.class})
    ProblemDetail unavailable(AuthenticationException ex) {
        String detail = ex instanceof LockedException
                ? "Too many failed attempts. Try again later."
                : "This account is disabled";
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException ex) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "The request is not valid");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    ProblemDetail status(org.springframework.web.server.ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return problem(status, status.getReasonPhrase(), ex.getReason() == null ? status.getReasonPhrase() : ex.getReason());
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    ProblemDetail constraints(jakarta.validation.ConstraintViolationException ex) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "The request is not valid");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable() {
        return problem(HttpStatus.BAD_REQUEST, "Bad request", "The request body is missing or malformed");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}