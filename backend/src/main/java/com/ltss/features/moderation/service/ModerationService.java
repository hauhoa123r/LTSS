package com.ltss.features.moderation.service;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.features.auth.repository.AuthorizationRepository;
import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.auth.service.AuditService;
import com.ltss.features.auth.service.ClientRequestInfo;
import com.ltss.features.content.entity.*;
import com.ltss.features.content.repository.*;
import com.ltss.features.community.entity.ReviewEntity;
import com.ltss.features.community.entity.ReviewStatus;
import com.ltss.features.community.repository.ReviewRepository;
import com.ltss.features.moderation.dto.*;
import com.ltss.features.moderation.entity.*;
import com.ltss.features.moderation.repository.ModerationRepository;
import com.ltss.features.moderation.repository.NotificationRepository;
import com.ltss.features.quiz.entity.QuizEntity;
import com.ltss.features.quiz.entity.QuizStatus;
import com.ltss.features.quiz.repository.QuizRepository;
import com.ltss.features.quiz.service.QuizAggregateValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ModerationService {
    private static final Set<String> MODERATOR_ROLES = Set.of("MODERATOR", "ADMINISTRATOR");

    private final ModerationRepository moderationRepository;
    private final NotificationRepository notificationRepository;
    private final ArticleRepository articleRepository;
    private final EventRepository eventRepository;
    private final BusinessPostRepository postRepository;
    private final PromotionRepository promotionRepository;
    private final BusinessRepository businessRepository;
    private final ReviewRepository reviewRepository;
    private final QuizRepository quizRepository;
    private final QuizAggregateValidator quizAggregateValidator;
    private final AuthorizationRepository authorizationRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final Clock clock;

    public ModerationService(
            ModerationRepository moderationRepository,
            NotificationRepository notificationRepository,
            ArticleRepository articleRepository,
            EventRepository eventRepository,
            BusinessPostRepository postRepository,
            PromotionRepository promotionRepository,
            BusinessRepository businessRepository,
            ReviewRepository reviewRepository,
            QuizRepository quizRepository,
            QuizAggregateValidator quizAggregateValidator,
            AuthorizationRepository authorizationRepository,
            CurrentUserService currentUserService,
            AuditService auditService,
            Clock clock
    ) {
        this.moderationRepository = moderationRepository;
        this.notificationRepository = notificationRepository;
        this.articleRepository = articleRepository;
        this.eventRepository = eventRepository;
        this.postRepository = postRepository;
        this.promotionRepository = promotionRepository;
        this.businessRepository = businessRepository;
        this.reviewRepository = reviewRepository;
        this.quizRepository = quizRepository;
        this.quizAggregateValidator = quizAggregateValidator;
        this.authorizationRepository = authorizationRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public ModerationRecordResponse submit(
            ModerationTargetType type,
            Long targetId,
            SubmitModerationRequest request,
            ClientRequestInfo requestInfo
    ) {
        Long actorId = currentUserService.requireUserId();
        Object target = lockedTarget(type, targetId);
        requireVersion(target, request.targetVersion());
        requireOwner(type, target, actorId);
        requireSubmittable(target);
        if (moderationRepository.findPending(type.name(), targetId).isPresent()) {
            throw new ConflictException("This target already has a pending moderation case");
        }

        Instant now = clock.instant();
        submitTarget(target, now);
        ModerationRecordEntity record = moderationRepository.save(ModerationRecordEntity.pending(
                actorId, type, targetId, normalize(request.note()), now
        ));
        notifyModerators(record, type, targetId, now, actorId);
        auditService.recordDomain(actorId, "MODERATION_SUBMITTED", type.name(), targetId, requestInfo);
        return response(record, target, request.targetVersion() + 1);
    }

    @Transactional
    public void registerPendingReview(Long reviewId, Long submitterId, ClientRequestInfo requestInfo) {
        if (moderationRepository.findPending(ModerationTargetType.REVIEW.name(), reviewId).isPresent()) {
            throw new ConflictException("This review already has a pending moderation case");
        }
        Instant now = clock.instant();
        ModerationRecordEntity record = moderationRepository.save(ModerationRecordEntity.pending(
                submitterId, ModerationTargetType.REVIEW, reviewId, null, now
        ));
        notifyModerators(record, ModerationTargetType.REVIEW, reviewId, now, submitterId);
        auditService.recordDomain(submitterId, "MODERATION_SUBMITTED", "REVIEW", reviewId, requestInfo);
    }

    @Transactional(readOnly = true)
    public PageResponse<ModerationRecordResponse> queue(ModerationTargetType type, int page, int size) {
        requireModerator(currentUserService.requireUserId());
        Page<ModerationRecordEntity> records = moderationRepository.queue(
                ModerationStatus.PENDING, type == null ? null : type.name(), PageRequest.of(page, size)
        );
        return PageResponse.from(records.map(this::response));
    }

    @Transactional(readOnly = true)
    public ModerationRecordResponse detail(Long caseId) {
        Long actorId = currentUserService.requireUserId();
        ModerationRecordEntity record = requireCase(caseId);
        if (!Objects.equals(record.getSubmittedByUserId(), actorId) && !isModerator(actorId)) {
            throw new AccessDeniedException("Moderation case is not accessible");
        }
        return response(record);
    }

    @Transactional(readOnly = true)
    public PageResponse<ModerationRecordResponse> history(
            ModerationTargetType type,
            Long targetId,
            int page,
            int size
    ) {
        Long actorId = currentUserService.requireUserId();
        Object target = target(type, targetId);
        if (!isOwner(type, target, actorId) && !isModerator(actorId)) {
            throw new AccessDeniedException("Moderation history is not accessible");
        }
        Page<ModerationRecordEntity> records = moderationRepository.history(
                type.name(), targetId, PageRequest.of(page, size)
        );
        return PageResponse.from(records.map(record -> response(record, target)));
    }

    @Transactional
    public ModerationRecordResponse approve(
            Long caseId,
            ModerationDecisionRequest request,
            ClientRequestInfo requestInfo
    ) {
        return decide(caseId, ModerationDecision.APPROVED, request, requestInfo);
    }

    @Transactional
    public ModerationRecordResponse reject(
            Long caseId,
            ModerationDecisionRequest request,
            ClientRequestInfo requestInfo
    ) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw new ConflictException("A rejection reason is required");
        }
        return decide(caseId, ModerationDecision.REJECTED, request, requestInfo);
    }

    @Transactional
    public ModerationRecordResponse cancel(
            Long caseId,
            CancelModerationRequest request,
            ClientRequestInfo requestInfo
    ) {
        Long actorId = currentUserService.requireUserId();
        ModerationRecordEntity record = lockedPendingCase(caseId);
        if (!Objects.equals(record.getSubmittedByUserId(), actorId)) {
            throw new AccessDeniedException("Only the submitter can cancel this case");
        }
        Object target = lockedTarget(record.targetType(), record.targetId());
        requireVersion(target, request.targetVersion());
        requirePending(target);

        cancelTarget(target);
        record.cancel(clock.instant());
        auditService.recordDomain(actorId, "MODERATION_CANCELLED", record.targetType().name(), record.targetId(), requestInfo);
        return response(record, target, request.targetVersion() + 1);
    }

    private ModerationRecordResponse decide(
            Long caseId,
            ModerationDecision decision,
            ModerationDecisionRequest request,
            ClientRequestInfo requestInfo
    ) {
        Long actorId = currentUserService.requireUserId();
        requireModerator(actorId);
        ModerationRecordEntity record = lockedPendingCase(caseId);
        Object target = lockedTarget(record.targetType(), record.targetId());
        requireVersion(target, request.targetVersion());
        requirePending(target);

        Instant now = clock.instant();
        if (decision == ModerationDecision.APPROVED) approveTarget(target, now);
        else rejectTarget(target);
        record.resolve(actorId, decision, normalize(request.reason()), now);
        notifySubmitter(record, decision, now);
        auditService.recordDomain(
                actorId,
                decision == ModerationDecision.APPROVED ? "MODERATION_APPROVED" : "MODERATION_REJECTED",
                record.targetType().name(), record.targetId(), requestInfo
        );
        return response(record, target, request.targetVersion() + 1);
    }

    private ModerationRecordEntity requireCase(Long id) {
        return moderationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Moderation case was not found"));
    }

    private ModerationRecordEntity lockedPendingCase(Long id) {
        ModerationRecordEntity record = moderationRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Moderation case was not found"));
        if (record.getStatus() != ModerationStatus.PENDING) {
            throw new ConflictException("Moderation case has already reached a terminal state");
        }
        if (record.targetType() == null) {
            throw new ConflictException("This target type is not supported in the current phase");
        }
        return record;
    }

    private Object lockedTarget(ModerationTargetType type, Long id) {
        return (switch (type) {
            case ARTICLE -> articleRepository.findLockedById(id).map(item -> (Object) item);
            case EVENT -> eventRepository.findLockedById(id).map(item -> (Object) item);
            case BUSINESS_POST -> postRepository.findLockedById(id).map(item -> (Object) item);
            case PROMOTION -> promotionRepository.findLockedById(id).map(item -> (Object) item);
            case QUIZ -> quizRepository.findLockedById(id).map(item -> (Object) item);
            case REVIEW -> reviewRepository.findLockedById(id).map(item -> (Object) item);
        }).orElseThrow(() -> new ResourceNotFoundException("Moderation target was not found"));
    }

    private Object target(ModerationTargetType type, Long id) {
        return (switch (type) {
            case ARTICLE -> articleRepository.findById(id).map(item -> (Object) item);
            case EVENT -> eventRepository.findById(id).map(item -> (Object) item);
            case BUSINESS_POST -> postRepository.findById(id).map(item -> (Object) item);
            case PROMOTION -> promotionRepository.findById(id).map(item -> (Object) item);
            case QUIZ -> quizRepository.findById(id).map(item -> (Object) item);
            case REVIEW -> reviewRepository.findById(id).map(item -> (Object) item);
        }).orElseThrow(() -> new ResourceNotFoundException("Moderation target was not found"));
    }

    private void requireOwner(ModerationTargetType type, Object target, Long actorId) {
        if (!isOwner(type, target, actorId)) throw new AccessDeniedException("Moderation target is not owned by this user");
    }

    private boolean isOwner(ModerationTargetType type, Object target, Long actorId) {
        return switch (type) {
            case ARTICLE -> Objects.equals(((ArticleEntity) target).getAuthorUserId(), actorId);
            case EVENT -> Objects.equals(((EventEntity) target).getCreatedByUserId(), actorId);
            case BUSINESS_POST -> ownsBusiness(((BusinessPostEntity) target).getBusinessId(), actorId);
            case PROMOTION -> ownsBusiness(((PromotionEntity) target).getBusinessId(), actorId);
            case QUIZ -> Objects.equals(((QuizEntity) target).getCreatedByUserId(), actorId);
            case REVIEW -> Objects.equals(((ReviewEntity) target).getUserId(), actorId);
        };
    }

    private boolean ownsBusiness(Long businessId, Long actorId) {
        return businessRepository.findById(businessId)
                .map(business -> Objects.equals(business.getOwnerUserId(), actorId))
                .orElse(false);
    }

    private void requireSubmittable(Object target) {
        boolean valid = target instanceof ArticleEntity article
                ? article.getStatus() == PublicationStatus.DRAFT || article.getStatus() == PublicationStatus.REJECTED
                : target instanceof EventEntity event
                ? event.getStatus() == EventStatus.DRAFT || event.getStatus() == EventStatus.REJECTED
                : target instanceof BusinessPostEntity post
                ? post.getStatus() == PublicationStatus.DRAFT || post.getStatus() == PublicationStatus.REJECTED
                : target instanceof PromotionEntity promotion
                ? promotion.getStatus() == PromotionStatus.DRAFT || promotion.getStatus() == PromotionStatus.REJECTED
                : target instanceof QuizEntity quiz
                ? quiz.getStatus() == QuizStatus.DRAFT || quiz.getStatus() == QuizStatus.REJECTED
                : false;
        if (!valid) throw new ConflictException("Target cannot be submitted from its current state");
    }

    private void requirePending(Object target) {
        boolean pending = target instanceof ArticleEntity article ? article.getStatus() == PublicationStatus.PENDING
                : target instanceof EventEntity event ? event.getStatus() == EventStatus.PENDING
                : target instanceof BusinessPostEntity post ? post.getStatus() == PublicationStatus.PENDING
                : target instanceof PromotionEntity promotion && promotion.getStatus() == PromotionStatus.PENDING;
        if (target instanceof QuizEntity quiz) pending = quiz.getStatus() == QuizStatus.PENDING;
        if (target instanceof ReviewEntity review) pending = review.getStatus() == ReviewStatus.PENDING;
        if (!pending) throw new ConflictException("Moderation target is not pending");
    }

    private void requireVersion(Object target, Integer expected) {
        if (!Objects.equals(version(target), expected)) {
            throw new ConflictException("Target was changed by another request; reload and try again");
        }
    }

    private Integer version(Object target) {
        if (target instanceof ArticleEntity item) return item.getVersion();
        if (target instanceof EventEntity item) return item.getVersion();
        if (target instanceof BusinessPostEntity item) return item.getVersion();
        if (target instanceof PromotionEntity item) return item.getVersion();
        if (target instanceof QuizEntity item) return item.getVersion();
        return ((ReviewEntity) target).getVersion();
    }

    private String title(Object target) {
        if (target instanceof ArticleEntity item) return item.getTitle();
        if (target instanceof EventEntity item) return item.getTitle();
        if (target instanceof BusinessPostEntity item) return item.getTitle();
        if (target instanceof PromotionEntity item) return item.getTitle();
        if (target instanceof QuizEntity item) return item.getTitle();
        return "Đánh giá #" + ((ReviewEntity) target).getId();
    }

    private void submitTarget(Object target, Instant now) {
        if (target instanceof ArticleEntity item) item.submit(now);
        else if (target instanceof EventEntity item) item.submit(now);
        else if (target instanceof BusinessPostEntity item) item.submit(now);
        else if (target instanceof PromotionEntity item) item.submit(now);
        else if (target instanceof QuizEntity item) {
            quizAggregateValidator.validate(item);
            item.submit(now);
        }
        else throw new ConflictException("Review resubmission is not available");
    }

    private void approveTarget(Object target, Instant now) {
        if (target instanceof ArticleEntity item) item.approve(now);
        else if (target instanceof EventEntity item) item.approve(now);
        else if (target instanceof BusinessPostEntity item) item.approve(now);
        else if (target instanceof PromotionEntity item) item.approve();
        else if (target instanceof QuizEntity item) {
            quizAggregateValidator.validate(item);
            item.approve(now);
        }
        else ((ReviewEntity) target).approve(now);
    }

    private void rejectTarget(Object target) {
        if (target instanceof ArticleEntity item) item.reject();
        else if (target instanceof EventEntity item) item.reject();
        else if (target instanceof BusinessPostEntity item) item.reject();
        else if (target instanceof PromotionEntity item) item.reject();
        else if (target instanceof QuizEntity item) item.reject();
        else ((ReviewEntity) target).reject();
    }

    private void cancelTarget(Object target) {
        if (target instanceof ArticleEntity item) item.cancelSubmission();
        else if (target instanceof EventEntity item) item.cancelSubmission();
        else if (target instanceof BusinessPostEntity item) item.cancelSubmission();
        else if (target instanceof PromotionEntity item) item.cancelSubmission();
        else if (target instanceof QuizEntity item) item.cancelSubmission();
        else ((ReviewEntity) target).cancelSubmission();
    }

    private boolean isModerator(Long userId) {
        return authorizationRepository.findEffectiveRoleCodes(userId).stream().anyMatch(MODERATOR_ROLES::contains);
    }

    private void requireModerator(Long userId) {
        if (!isModerator(userId)) throw new AccessDeniedException("Moderator role is required");
    }

    private void notifyModerators(
            ModerationRecordEntity record,
            ModerationTargetType type,
            Long targetId,
            Instant now,
            Long actorId
    ) {
        List<NotificationEntity> notifications = authorizationRepository
                .findActiveUserIdsWithDirectRoles(MODERATOR_ROLES).stream()
                .filter(userId -> !Objects.equals(userId, actorId))
                .map(userId -> new NotificationEntity(
                        userId, "Nội dung mới chờ duyệt",
                        type.name() + " #" + targetId + " vừa được gửi duyệt.",
                        "/moderation", now
                )).toList();
        notificationRepository.saveAll(notifications);
    }

    private void notifySubmitter(ModerationRecordEntity record, ModerationDecision decision, Instant now) {
        if (record.getSubmittedByUserId() == null) return;
        notificationRepository.save(new NotificationEntity(
                record.getSubmittedByUserId(),
                decision == ModerationDecision.APPROVED ? "Nội dung đã được duyệt" : "Nội dung bị từ chối",
                record.targetType().name() + " #" + record.targetId() + " đã có kết quả kiểm duyệt.",
                "/notifications", now
        ));
    }

    private ModerationRecordResponse response(ModerationRecordEntity record) {
        Object target = target(record.targetType(), record.targetId());
        return response(record, target);
    }

    private ModerationRecordResponse response(ModerationRecordEntity record, Object target) {
        return response(record, target, version(target));
    }

    private ModerationRecordResponse response(ModerationRecordEntity record, Object target, Integer targetVersion) {
        return new ModerationRecordResponse(
                record.getId(), record.targetType(), record.targetId(), title(target), targetVersion,
                record.getSubmittedByUserId(), record.getModeratorUserId(), record.getStatus(), record.getDecision(),
                record.getSubmissionNote(), record.getDecisionReason(), record.getSubmittedAt(), record.getResolvedAt()
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
