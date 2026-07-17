package com.ltss.features.auth.exception;

import com.ltss.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class AccountException extends ApplicationException {
    public AccountException(String code, HttpStatus status, String message) {
        super(code, status, message);
    }

    public static AccountException conflict(String message) {
        return new AccountException("ACCOUNT_CONFLICT", HttpStatus.CONFLICT, message);
    }

    public static AccountException invalidCredentials() {
        return new AccountException(
                "INVALID_CREDENTIALS",
                HttpStatus.UNAUTHORIZED,
                "Email or password is incorrect"
        );
    }

    public static AccountException invalidToken() {
        return new AccountException(
                "INVALID_ACCOUNT_TOKEN",
                HttpStatus.BAD_REQUEST,
                "The account token is invalid or has expired"
        );
    }

    public static AccountException rateLimited() {
        return new AccountException(
                "TOKEN_REQUEST_RATE_LIMITED",
                HttpStatus.TOO_MANY_REQUESTS,
                "Please wait before requesting another security message"
        );
    }

    public static AccountException forbiddenState() {
        return new AccountException(
                "ACCOUNT_UNAVAILABLE",
                HttpStatus.FORBIDDEN,
                "This account cannot access authenticated functions"
        );
    }
}
