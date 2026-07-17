package com.ltss.service.moderation.strategy;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.entity.content.PromotionEntity;
import com.ltss.entity.content.PromotionStatus;
import com.ltss.entity.moderation.ModerationTargetType;
import com.ltss.repository.content.BusinessRepository;
import com.ltss.repository.content.PromotionRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
public class PromotionModerationStrategy extends AbstractSubmittableModerationTargetStrategy<PromotionEntity> {
    private final PromotionRepository repository;
    private final BusinessRepository businessRepository;

    public PromotionModerationStrategy(PromotionRepository repository, BusinessRepository businessRepository) { super(PromotionEntity.class); this.repository = repository; this.businessRepository = businessRepository; }
    @Override public ModerationTargetType type() { return ModerationTargetType.PROMOTION; }
    @Override public Object find(Long id) { return repository.findById(id).orElseThrow(this::notFound); }
    @Override public Object findLocked(Long id) { return repository.findLockedById(id).orElseThrow(this::notFound); }
    @Override protected boolean isOwnerTarget(PromotionEntity target, Long actorId) { return businessRepository.findById(target.getBusinessId()).map(b -> Objects.equals(b.getOwnerUserId(), actorId)).orElse(false); }
    @Override protected void requireSubmittableTarget(PromotionEntity target) { if (target.getStatus() != PromotionStatus.DRAFT && target.getStatus() != PromotionStatus.REJECTED) throw new ConflictException("Target cannot be submitted from its current state"); }
    @Override protected boolean isPendingTarget(PromotionEntity target) { return target.getStatus() == PromotionStatus.PENDING; }
    @Override protected Integer versionTarget(PromotionEntity target) { return target.getVersion(); }
    @Override protected String titleTarget(PromotionEntity target) { return target.getTitle(); }
    @Override protected void submitTarget(PromotionEntity target, Instant now) { target.submit(now); }
    @Override protected void approveTarget(PromotionEntity target, Instant now) { target.approve(); }
    @Override protected void rejectTarget(PromotionEntity target) { target.reject(); }
    @Override protected void cancelTarget(PromotionEntity target) { target.cancelSubmission(); }
    private ResourceNotFoundException notFound() { return new ResourceNotFoundException("Moderation target was not found"); }
}
