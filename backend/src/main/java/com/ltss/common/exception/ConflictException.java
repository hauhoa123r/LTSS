package com.ltss.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApplicationException {

    public ConflictException(String message) {
        super("CONFLICT", HttpStatus.CONFLICT, message);
    }
}
