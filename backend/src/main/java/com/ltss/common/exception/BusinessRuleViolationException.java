package com.ltss.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleViolationException extends ApplicationException {

    public BusinessRuleViolationException(String message) {
        super("BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
