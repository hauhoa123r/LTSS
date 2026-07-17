package com.ltss.dto.moderation;

import com.ltss.entity.moderation.NotificationEntity;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        String type,
        String actionUrl,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationResponse from(NotificationEntity item) {
        return new NotificationResponse(
                item.getId(), item.getTitle(), item.getMessage(), item.getNotificationType(),
                item.getActionUrl(), item.isRead(), item.getReadAt(), item.getCreatedAt()
        );
    }
}
