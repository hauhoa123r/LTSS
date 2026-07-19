package com.ltss.security.auth;

import com.ltss.config.RefreshCookieProperties;
import com.ltss.config.auth.AccountProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieService {
    private final RefreshCookieProperties cookieProperties;
    private final AccountProperties accountProperties;

    public RefreshCookieService(
            RefreshCookieProperties cookieProperties,
            AccountProperties accountProperties) {
        this.cookieProperties = cookieProperties;
        this.accountProperties = accountProperties;
    }

    public String name() {
        return cookieProperties.name();
    }

    public ResponseCookie create(String token) {
        return baseCookie(token)
                .maxAge(accountProperties.refreshTokenLifetime())
                .build();
    }

    public ResponseCookie clear() {
        return baseCookie("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieProperties.name(), value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(cookieProperties.path());
    }
}
