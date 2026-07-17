package com.ltss.service.moderation.strategy;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.entity.content.EventEntity;
import com.ltss.entity.content.EventStatus;
import com.ltss.entity.moderation.ModerationTargetType;
import com.ltss.repository.content.EventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
public class EventModerationStrategy extends AbstractSubmittableModerationTargetStrategy<EventEntity> {
    private final EventRepository repository;

    public EventModerationStrategy(EventRepository repository) { super(EventEntity.class); this.repository = repository; }
    @Override public ModerationTargetType type() { return ModerationTargetType.EVENT; }
    @Override public Object find(Long id) { return repository.findById(id).orElseThrow(this::notFound); }
    @Override public Object findLocked(Long id) { return repository.findLockedById(id).orElseThrow(this::notFound); }
    @Override protected boolean isOwnerTarget(EventEntity target, Long actorId) { return Objects.equals(target.getCreatedByUserId(), actorId); }
    @Override protected void requireSubmittableTarget(EventEntity target) { if (target.getStatus() != EventStatus.DRAFT && target.getStatus() != EventStatus.REJECTED) throw new ConflictException("Target cannot be submitted from its current state"); }
    @Override protected boolean isPendingTarget(EventEntity target) { return target.getStatus() == EventStatus.PENDING; }
    @Override protected Integer versionTarget(EventEntity target) { return target.getVersion(); }
    @Override protected String titleTarget(EventEntity target) { return target.getTitle(); }
    @Override protected void submitTarget(EventEntity target, Instant now) { target.submit(now); }
    @Override protected void approveTarget(EventEntity target, Instant now) { target.approve(now); }
    @Override protected void rejectTarget(EventEntity target) { target.reject(); }
    @Override protected void cancelTarget(EventEntity target) { target.cancelSubmission(); }
    private ResourceNotFoundException notFound() { return new ResourceNotFoundException("Moderation target was not found"); }
}
