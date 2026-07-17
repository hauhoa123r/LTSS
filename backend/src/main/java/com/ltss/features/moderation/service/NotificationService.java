package com.ltss.features.moderation.service;

import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.moderation.dto.NotificationResponse;
import com.ltss.features.moderation.dto.UnreadNotificationCountResponse;
import com.ltss.features.moderation.entity.NotificationEntity;
import com.ltss.features.moderation.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public NotificationService(NotificationRepository repository, CurrentUserService currentUserService, Clock clock) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(int page, int size) {
        return PageResponse.from(repository.findAllByRecipientUserIdOrderByCreatedAtDesc(
                currentUserService.requireUserId(), PageRequest.of(page, size)
        ).map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse unreadCount() {
        return new UnreadNotificationCountResponse(
                repository.countByRecipientUserIdAndReadFalse(currentUserService.requireUserId())
        );
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        NotificationEntity notification = repository.findByIdAndRecipientUserId(
                notificationId, currentUserService.requireUserId()
        ).orElseThrow(() -> new ResourceNotFoundException("Notification was not found"));
        notification.markRead(clock.instant());
        return NotificationResponse.from(notification);
    }
}
