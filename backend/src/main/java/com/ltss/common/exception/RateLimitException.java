package com.ltss.common.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends ApplicationException {
    public RateLimitException(String message) {
        super("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
