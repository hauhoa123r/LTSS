package com.ltss.service.moderation.strategy;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.entity.content.BusinessPostEntity;
import com.ltss.entity.content.PublicationStatus;
import com.ltss.entity.moderation.ModerationTargetType;
import com.ltss.repository.content.BusinessPostRepository;
import com.ltss.repository.content.BusinessRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
public class BusinessPostModerationStrategy extends AbstractSubmittableModerationTargetStrategy<BusinessPostEntity> {
    private final BusinessPostRepository repository;
    private final BusinessRepository businessRepository;

    public BusinessPostModerationStrategy(BusinessPostRepository repository, BusinessRepository businessRepository) { super(BusinessPostEntity.class); this.repository = repository; this.businessRepository = businessRepository; }
    @Override public ModerationTargetType type() { return ModerationTargetType.BUSINESS_POST; }
    @Override public Object find(Long id) { return repository.findById(id).orElseThrow(this::notFound); }
    @Override public Object findLocked(Long id) { return repository.findLockedById(id).orElseThrow(this::notFound); }
    @Override protected boolean isOwnerTarget(BusinessPostEntity target, Long actorId) { return businessRepository.findById(target.getBusinessId()).map(b -> Objects.equals(b.getOwnerUserId(), actorId)).orElse(false); }
    @Override protected void requireSubmittableTarget(BusinessPostEntity target) { if (target.getStatus() != PublicationStatus.DRAFT && target.getStatus() != PublicationStatus.REJECTED) throw new ConflictException("Target cannot be submitted from its current state"); }
    @Override protected boolean isPendingTarget(BusinessPostEntity target) { return target.getStatus() == PublicationStatus.PENDING; }
    @Override protected Integer versionTarget(BusinessPostEntity target) { return target.getVersion(); }
    @Override protected String titleTarget(BusinessPostEntity target) { return target.getTitle(); }
    @Override protected void submitTarget(BusinessPostEntity target, Instant now) { target.submit(now); }
    @Override protected void approveTarget(BusinessPostEntity target, Instant now) { target.approve(now); }
    @Override protected void rejectTarget(BusinessPostEntity target) { target.reject(); }
    @Override protected void cancelTarget(BusinessPostEntity target) { target.cancelSubmission(); }
    private ResourceNotFoundException notFound() { return new ResourceNotFoundException("Moderation target was not found"); }
}
