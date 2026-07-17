package com.ltss.service.moderation;

import com.ltss.service.moderation.impl.NotificationServiceImpl;

import com.ltss.security.auth.CurrentUserService;
import com.ltss.entity.moderation.NotificationEntity;
import com.ltss.repository.moderation.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock private NotificationRepository repository;
    @Mock private CurrentUserService currentUserService;

    @Test
    void markReadUsesRecipientScopedLookup() {
        Instant now = Instant.parse("2026-07-16T06:00:00Z");
        NotificationEntity notification = mock(NotificationEntity.class);
        when(currentUserService.requireUserId()).thenReturn(10L);
        when(repository.findByIdAndRecipientUserId(8L, 10L)).thenReturn(Optional.of(notification));
        NotificationService service = new NotificationServiceImpl(
                repository, currentUserService, Clock.fixed(now, ZoneOffset.UTC)
        );

        service.markRead(8L);

        verify(notification).markRead(now);
        verify(repository, never()).findById(8L);
    }
}
