package com.ltss.service.moderation.impl;

import com.ltss.service.moderation.NotificationService;

import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.dto.moderation.NotificationResponse;
import com.ltss.dto.moderation.UnreadNotificationCountResponse;
import com.ltss.entity.moderation.NotificationEntity;
import com.ltss.repository.moderation.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository repository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public NotificationServiceImpl(NotificationRepository repository, CurrentUserService currentUserService, Clock clock) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<NotificationResponse> list(int page, int size) {
        return PageResponse.from(repository.findAllByRecipientUserIdOrderByCreatedAtDesc(
                currentUserService.requireUserId(), PageRequest.of(page, size)
        ).map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    @Override
    public UnreadNotificationCountResponse unreadCount() {
        return new UnreadNotificationCountResponse(
                repository.countByRecipientUserIdAndReadFalse(currentUserService.requireUserId())
        );
    }

    @Transactional
    @Override
    public NotificationResponse markRead(Long notificationId) {
        NotificationEntity notification = repository.findByIdAndRecipientUserId(
                notificationId, currentUserService.requireUserId()
        ).orElseThrow(() -> new ResourceNotFoundException("Notification was not found"));
        notification.markRead(clock.instant());
        return NotificationResponse.from(notification);
    }
}
