package com.ltss.features.moderation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Column(name = "notification_type", nullable = false, length = 40)
    private String notificationType;
    @Column(name = "action_url", length = 1000)
    private String actionUrl;
    @Column(name = "is_read", nullable = false)
    private boolean read;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public NotificationEntity(Long recipientUserId, String title, String message, String actionUrl, Instant now) {
        this.recipientUserId = recipientUserId;
        this.title = title;
        this.message = message;
        this.notificationType = "MODERATION";
        this.actionUrl = actionUrl;
        this.createdAt = now;
    }

    public void markRead(Instant now) {
        if (read) return;
        read = true;
        readAt = now;
    }
}
