package com.ltss.service.moderation.strategy;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.entity.moderation.ModerationTargetType;
import com.ltss.entity.quiz.QuizEntity;
import com.ltss.entity.quiz.QuizStatus;
import com.ltss.repository.quiz.QuizRepository;
import com.ltss.service.quiz.QuizAggregateValidator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
public class QuizModerationStrategy extends AbstractSubmittableModerationTargetStrategy<QuizEntity> {
    private final QuizRepository repository;
    private final QuizAggregateValidator validator;

    public QuizModerationStrategy(QuizRepository repository, QuizAggregateValidator validator) { super(QuizEntity.class); this.repository = repository; this.validator = validator; }
    @Override public ModerationTargetType type() { return ModerationTargetType.QUIZ; }
    @Override public Object find(Long id) { return repository.findById(id).orElseThrow(this::notFound); }
    @Override public Object findLocked(Long id) { return repository.findLockedById(id).orElseThrow(this::notFound); }
    @Override protected boolean isOwnerTarget(QuizEntity target, Long actorId) { return Objects.equals(target.getCreatedByUserId(), actorId); }
    @Override protected void requireSubmittableTarget(QuizEntity target) { if (target.getStatus() != QuizStatus.DRAFT && target.getStatus() != QuizStatus.REJECTED) throw new ConflictException("Target cannot be submitted from its current state"); }
    @Override protected boolean isPendingTarget(QuizEntity target) { return target.getStatus() == QuizStatus.PENDING; }
    @Override protected Integer versionTarget(QuizEntity target) { return target.getVersion(); }
    @Override protected String titleTarget(QuizEntity target) { return target.getTitle(); }
    @Override protected void submitTarget(QuizEntity target, Instant now) { validator.validate(target); target.submit(now); }
    @Override protected void approveTarget(QuizEntity target, Instant now) { validator.validate(target); target.approve(now); }
    @Override protected void rejectTarget(QuizEntity target) { target.reject(); }
    @Override protected void cancelTarget(QuizEntity target) { target.cancelSubmission(); }
    private ResourceNotFoundException notFound() { return new ResourceNotFoundException("Moderation target was not found"); }
}
