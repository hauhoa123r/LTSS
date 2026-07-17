package com.ltss.security.auth;

import com.ltss.entity.auth.UserStatus;
import com.ltss.repository.auth.UserRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class JwtAccountStatusValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_ACCOUNT = new OAuth2Error(
            "invalid_token",
            "The account is unavailable",
            null
    );

    private final UserRepository userRepository;
    private final Clock clock;

    public JwtAccountStatusValidator(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            Long userId = Long.valueOf(jwt.getSubject());
            boolean valid = userRepository.findById(userId)
                    .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                    .filter(user -> user.getLockedUntil() == null || !user.getLockedUntil().isAfter(clock.instant()))
                    .isPresent();
            return valid
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        } catch (NumberFormatException exception) {
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }
    }
}
