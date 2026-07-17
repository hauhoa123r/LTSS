package com.ltss.service.moderation.strategy;

import com.ltss.entity.moderation.ModerationTargetType;

import java.time.Instant;

public interface ModerationTargetStrategy {
    ModerationTargetType type();

    Object find(Long targetId);

    Object findLocked(Long targetId);

    boolean isOwner(Object target, Long actorId);

    boolean isPending(Object target);

    Integer version(Object target);

    String title(Object target);

    void approve(Object target, Instant now);

    void reject(Object target);

    void cancel(Object target);
}
