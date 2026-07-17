package com.ltss.config.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("ltss.account")
public record AccountProperties(
        @Valid @NotNull Email email,
        @NotNull Duration verificationTokenLifetime,
        @NotNull Duration refreshTokenLifetime,
        @NotNull Duration securityTokenLifetime,
        @NotNull Duration tokenResendDelay,
        @NotNull Duration temporaryLockDuration,
        @Min(1) int maxLoginFailures
) {
    public record Email(
            boolean enabled,
            @NotBlank String from,
            @NotBlank String frontendBaseUrl
    ) {
    }
}
