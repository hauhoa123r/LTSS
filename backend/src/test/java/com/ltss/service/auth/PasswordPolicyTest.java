package com.ltss.service.auth;

import com.ltss.common.exception.BusinessRuleViolationException;
import com.ltss.entity.auth.PasswordChangeReason;
import com.ltss.entity.auth.PasswordHistoryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final PasswordPolicy policy = new PasswordPolicy(encoder);

    @Test
    void acceptsStrongPasswordWithoutIdentityData() {
        assertThatCode(() -> policy.validateForIdentity(
                "River!Stone9", "visitor@example.com", "Nguyen Van An", "An Nguyen"
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordContainingEmailIdentity() {
        assertThatThrownBy(() -> policy.validateForIdentity(
                "Visitor!2026", "visitor@example.com", "Nguyen Van An", "An Nguyen"
        )).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rejectsCurrentOrRecentPassword() {
        String currentHash = encoder.encode("Current!Pass9");
        PasswordHistoryEntity previous = new PasswordHistoryEntity(
                1L,
                encoder.encode("Previous!Pass8"),
                PasswordChangeReason.USER_CHANGE,
                Instant.now()
        );

        assertThatThrownBy(() -> policy.validateNotReused(
                "Previous!Pass8", currentHash, List.of(previous)
        )).isInstanceOf(BusinessRuleViolationException.class);
    }
}
