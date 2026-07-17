package com.ltss.service.moderation.strategy;

import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.entity.community.ReviewEntity;
import com.ltss.entity.community.ReviewStatus;
import com.ltss.entity.moderation.ModerationTargetType;
import com.ltss.repository.community.ReviewRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
public class ReviewModerationStrategy extends AbstractModerationTargetStrategy<ReviewEntity> {
    private final ReviewRepository repository;

    public ReviewModerationStrategy(ReviewRepository repository) { super(ReviewEntity.class); this.repository = repository; }
    @Override public ModerationTargetType type() { return ModerationTargetType.REVIEW; }
    @Override public Object find(Long id) { return repository.findById(id).orElseThrow(this::notFound); }
    @Override public Object findLocked(Long id) { return repository.findLockedById(id).orElseThrow(this::notFound); }
    @Override protected boolean isOwnerTarget(ReviewEntity target, Long actorId) { return Objects.equals(target.getUserId(), actorId); }
    @Override protected boolean isPendingTarget(ReviewEntity target) { return target.getStatus() == ReviewStatus.PENDING; }
    @Override protected Integer versionTarget(ReviewEntity target) { return target.getVersion(); }
    @Override protected String titleTarget(ReviewEntity target) { return "Đánh giá #" + target.getId(); }
    @Override protected void approveTarget(ReviewEntity target, Instant now) { target.approve(now); }
    @Override protected void rejectTarget(ReviewEntity target) { target.reject(); }
    @Override protected void cancelTarget(ReviewEntity target) { target.cancelSubmission(); }
    private ResourceNotFoundException notFound() { return new ResourceNotFoundException("Moderation target was not found"); }
}
