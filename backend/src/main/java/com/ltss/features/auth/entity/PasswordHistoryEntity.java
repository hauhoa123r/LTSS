package com.ltss.features.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "password_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_reason", nullable = false, length = 30)
    private PasswordChangeReason changeReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PasswordHistoryEntity(Long userId, String passwordHash, PasswordChangeReason reason, Instant createdAt) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.changeReason = reason;
        this.createdAt = createdAt;
    }

    public String passwordHash() {
        return passwordHash;
    }
}
