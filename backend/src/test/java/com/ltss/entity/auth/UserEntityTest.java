package com.ltss.entity.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    @Test
    void registrationStartsPendingAndVerificationActivatesAccount() {
        UserEntity user = new UserEntity("Tourist", "Tourist", "tourist@example.com", "hash");
        Instant verifiedAt = Instant.now();

        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        user.verifyEmail(verifiedAt);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getEmailVerifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    void accountLocksOnlyAfterMoreThanFiveFailures() {
        UserEntity user = new UserEntity("Tourist", "Tourist", "tourist@example.com", "hash");
        Instant lockUntil = Instant.now().plus(15, ChronoUnit.MINUTES);

        for (int attempt = 0; attempt < 5; attempt++) {
            user.recordFailedLogin(5, lockUntil);
        }
        assertThat(user.getLockedUntil()).isNull();

        user.recordFailedLogin(5, lockUntil);
        assertThat(user.getLockedUntil()).isEqualTo(lockUntil);
    }
}
