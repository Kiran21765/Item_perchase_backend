package com.example.itempurchase.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Business/validation error that is shown to the client as-is. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final transient Map<String, Object> details;

    public ApiException(HttpStatus status, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, null);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message, null);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message, null);
    }

    public static ApiException conflict(String message, Map<String, Object> details) {
        return new ApiException(HttpStatus.CONFLICT, message, details);
    }

    public HttpStatus getStatus() { return status; }
    public Map<String, Object> getDetails() { return details; }
}
