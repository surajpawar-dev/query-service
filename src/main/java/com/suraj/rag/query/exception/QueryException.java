package com.suraj.rag.query.exception;

import org.springframework.http.HttpStatus;

public class QueryException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public QueryException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
