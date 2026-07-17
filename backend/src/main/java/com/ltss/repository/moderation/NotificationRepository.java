package com.ltss.repository.moderation;

import com.ltss.entity.moderation.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findAllByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);
    long countByRecipientUserIdAndReadFalse(Long recipientUserId);
    Optional<NotificationEntity> findByIdAndRecipientUserId(Long id, Long recipientUserId);
}
