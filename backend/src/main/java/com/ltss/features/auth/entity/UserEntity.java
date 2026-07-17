package com.ltss.features.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    @Column(length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "failed_login_count", nullable = false)
    private short failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deactivated_by_user_id")
    private Long deactivatedByUserId;

    @Version
    @Column(nullable = false)
    private Integer version;

    public UserEntity(String fullName, String displayName, String email, String passwordHash) {
        this.fullName = fullName;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = UserStatus.PENDING_VERIFICATION;
        this.failedLoginCount = 0;
        this.version = 0;
    }

    public void verifyEmail(Instant now) {
        emailVerifiedAt = now;
        status = UserStatus.ACTIVE;
    }

    public void recordSuccessfulLogin(Instant now) {
        failedLoginCount = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    public void recordFailedLogin(int maximumFailures, Instant lockUntil) {
        failedLoginCount++;
        if (failedLoginCount > maximumFailures) {
            lockedUntil = lockUntil;
        }
    }

    public void updateProfile(String fullName, String displayName, String phone, String address) {
        this.fullName = fullName;
        this.displayName = displayName;
        this.phone = phone;
        this.address = address;
    }

    public void changePassword(String encodedPassword, Instant now) {
        passwordHash = encodedPassword;
        passwordChangedAt = now;
        failedLoginCount = 0;
        lockedUntil = null;
    }

    public void changeAdministrativeStatus(UserStatus targetStatus, Long administratorId, Instant now) {
        status = targetStatus;
        if (targetStatus == UserStatus.ACTIVE) {
            deactivatedAt = null;
            deactivatedByUserId = null;
            failedLoginCount = 0;
            lockedUntil = null;
        } else {
            deactivatedAt = now;
            deactivatedByUserId = administratorId;
        }
    }
}
