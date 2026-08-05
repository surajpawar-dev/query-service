package com.suraj.rag.query.exception;

import com.suraj.rag.query.common.HeaderNames;
import com.suraj.rag.query.common.MdcKeys;
import com.suraj.rag.query.dto.ErrorResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WebExchangeBindException.class)
    ResponseEntity<ErrorResponse> handleValidation(
            WebExchangeBindException exception, ServerWebExchange exchange) {
        String message =
                exception.getFieldErrors().stream()
                        .findFirst()
                        .map(error -> error.getField() + " " + error.getDefaultMessage())
                        .orElse("Request validation failed");
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message, exchange);
    }

    @ExceptionHandler(QueryException.class)
    ResponseEntity<ErrorResponse> handleQueryException(
            QueryException exception, ServerWebExchange exchange) {
        log.warn(
                "Handled query exception: code={}, path={}, message={}",
                exception.errorCode(),
                exchange.getRequest().getPath().value(),
                exception.getMessage());
        return build(
                exception.httpStatus(), exception.errorCode(), exception.getMessage(), exchange);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnknown(Exception exception, ServerWebExchange exchange) {
        log.error(
                "Unhandled query service exception: path={}",
                exchange.getRequest().getPath().value(),
                exception);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "Unexpected query service failure",
                exchange);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, ErrorCode errorCode, String message, ServerWebExchange exchange) {
        return ResponseEntity.status(status)
                .body(
                        new ErrorResponse(
                                Instant.now(),
                                status.value(),
                                errorCode.name(),
                                message,
                                exchange.getRequest().getPath().value(),
                                resolveCorrelationId(exchange)));
    }

    private String resolveCorrelationId(ServerWebExchange exchange) {
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        return correlationId == null || correlationId.isBlank()
                ? exchange.getRequest().getHeaders().getFirst(HeaderNames.CORRELATION_ID)
                : correlationId;
    }
}
