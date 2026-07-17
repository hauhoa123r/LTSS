package com.ltss.service.moderation.strategy;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.entity.content.ArticleEntity;
import com.ltss.entity.content.PublicationStatus;
import com.ltss.entity.moderation.ModerationTargetType;
import com.ltss.repository.content.ArticleRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
public class ArticleModerationStrategy extends AbstractSubmittableModerationTargetStrategy<ArticleEntity> {
    private final ArticleRepository repository;

    public ArticleModerationStrategy(ArticleRepository repository) {
        super(ArticleEntity.class);
        this.repository = repository;
    }

    @Override public ModerationTargetType type() { return ModerationTargetType.ARTICLE; }
    @Override public Object find(Long id) { return repository.findById(id).orElseThrow(this::notFound); }
    @Override public Object findLocked(Long id) { return repository.findLockedById(id).orElseThrow(this::notFound); }
    @Override protected boolean isOwnerTarget(ArticleEntity target, Long actorId) { return Objects.equals(target.getAuthorUserId(), actorId); }
    @Override protected void requireSubmittableTarget(ArticleEntity target) { requireDraftOrRejected(target.getStatus()); }
    @Override protected boolean isPendingTarget(ArticleEntity target) { return target.getStatus() == PublicationStatus.PENDING; }
    @Override protected Integer versionTarget(ArticleEntity target) { return target.getVersion(); }
    @Override protected String titleTarget(ArticleEntity target) { return target.getTitle(); }
    @Override protected void submitTarget(ArticleEntity target, Instant now) { target.submit(now); }
    @Override protected void approveTarget(ArticleEntity target, Instant now) { target.approve(now); }
    @Override protected void rejectTarget(ArticleEntity target) { target.reject(); }
    @Override protected void cancelTarget(ArticleEntity target) { target.cancelSubmission(); }

    private void requireDraftOrRejected(PublicationStatus status) {
        if (status != PublicationStatus.DRAFT && status != PublicationStatus.REJECTED) {
            throw new ConflictException("Target cannot be submitted from its current state");
        }
    }

    private ResourceNotFoundException notFound() { return new ResourceNotFoundException("Moderation target was not found"); }
}
