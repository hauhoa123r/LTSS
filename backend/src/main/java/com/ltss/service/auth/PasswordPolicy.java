package com.ltss.service.auth;

import com.ltss.common.exception.BusinessRuleViolationException;
import com.ltss.entity.auth.PasswordHistoryEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class PasswordPolicy {
    private static final Set<String> WEAK_PASSWORDS = Set.of(
            "password", "password1", "password123", "12345678", "123456789",
            "qwerty123", "abc12345", "admin123", "letmein1", "welcome1"
    );

    private final PasswordEncoder passwordEncoder;

    public PasswordPolicy(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void validateForIdentity(String password, String email, String fullName, String displayName) {
        if (password == null || password.length() < 8 || password.length() > 32) {
            reject("Password must contain between 8 and 32 characters");
        }
        if (password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().allMatch(Character::isLetterOrDigit)) {
            reject("Password must include uppercase, lowercase, number, and special characters");
        }

        String normalizedPassword = normalize(password);
        if (WEAK_PASSWORDS.contains(normalizedPassword)) {
            reject("Password is too common");
        }

        String localPart = email == null ? "" : email.split("@", 2)[0];
        if (containsIdentity(normalizedPassword, localPart)
                || containsIdentity(normalizedPassword, fullName)
                || containsIdentity(normalizedPassword, displayName)) {
            reject("Password must not contain personal account information");
        }
    }

    public void validateNotReused(
            String password,
            String currentHash,
            List<PasswordHistoryEntity> recentPasswords
    ) {
        if (passwordEncoder.matches(password, currentHash)
                || recentPasswords.stream().anyMatch(item -> passwordEncoder.matches(password, item.passwordHash()))) {
            reject("New password must differ from the current and last three passwords");
        }
    }

    private boolean containsIdentity(String password, String identity) {
        String normalizedIdentity = normalize(identity).replace(" ", "");
        return normalizedIdentity.length() >= 3 && password.replace(" ", "").contains(normalizedIdentity);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private void reject(String message) {
        throw new BusinessRuleViolationException(message);
    }
}
