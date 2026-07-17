package com.ltss.service.moderation.impl;

import com.ltss.service.moderation.ModerationService;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.mapper.moderation.ModerationTargetContentMapper;
import com.ltss.repository.auth.AuthorizationRepository;
import com.ltss.repository.auth.UserRepository;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.service.auth.AuditService;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.moderation.CancelModerationRequest;
import com.ltss.dto.moderation.ModerationDecisionRequest;
import com.ltss.dto.moderation.ModerationRecordResponse;
import com.ltss.dto.moderation.ModerationTargetContentResponse;
import com.ltss.dto.moderation.SubmitModerationRequest;
import com.ltss.entity.moderation.ModerationDecision;
import com.ltss.entity.moderation.ModerationRecordEntity;
import com.ltss.entity.moderation.ModerationStatus;
import com.ltss.entity.moderation.ModerationTargetType;
import com.ltss.entity.moderation.NotificationEntity;
import com.ltss.repository.moderation.ModerationRepository;
import com.ltss.repository.moderation.NotificationRepository;
import com.ltss.service.moderation.strategy.ModerationTargetStrategy;
import com.ltss.service.moderation.strategy.ModerationTargetStrategyRegistry;
import com.ltss.service.moderation.strategy.SubmittableModerationTargetStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ModerationServiceImpl implements ModerationService {
    private static final Set<String> MODERATOR_ROLES = Set.of("MODERATOR", "ADMINISTRATOR");

    private final ModerationRepository moderationRepository;
    private final NotificationRepository notificationRepository;
    private final ModerationTargetStrategyRegistry targetStrategies;
    private final ModerationTargetContentMapper targetContentMapper;
    private final AuthorizationRepository authorizationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final Clock clock;

    public ModerationServiceImpl(
            ModerationRepository moderationRepository,
            NotificationRepository notificationRepository,
            ModerationTargetStrategyRegistry targetStrategies,
            ModerationTargetContentMapper targetContentMapper,
            AuthorizationRepository authorizationRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            AuditService auditService,
            Clock clock
    ) {
        this.moderationRepository = moderationRepository;
        this.notificationRepository = notificationRepository;
        this.targetStrategies = targetStrategies;
        this.targetContentMapper = targetContentMapper;
        this.authorizationRepository = authorizationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    @Override
    public ModerationRecordResponse submit(
            ModerationTargetType type,
            Long targetId,
            SubmitModerationRequest request,
            ClientRequestInfo requestInfo
    ) {
        Long actorId = currentUserService.requireUserId();
        SubmittableModerationTargetStrategy targetStrategy = targetStrategies.requireSubmittable(type);
        Object target = lockedTarget(type, targetId);
        requireVersion(targetStrategy, target, request.targetVersion());
        requireOwner(targetStrategy, target, actorId);
        targetStrategy.requireSubmittable(target);
        if (moderationRepository.findPending(type.name(), targetId).isPresent()) {
            throw new ConflictException("This target already has a pending moderation case");
        }

        Instant now = clock.instant();
        Map<String, Object> targetSnapshot = targetContentMapper.snapshot(target);
        targetStrategy.submit(target, now);
        ModerationRecordEntity record = moderationRepository.save(ModerationRecordEntity.pending(
                actorId, type, targetId, normalize(request.note()), targetSnapshot, now
        ));
        notifyModerators(record, type, targetId, now, actorId);
        auditService.recordDomain(actorId, "MODERATION_SUBMITTED", type.name(), targetId, requestInfo);
        return response(record, target, request.targetVersion() + 1);
    }

    @Transactional
    @Override
    public void registerPendingReview(Long reviewId, Long submitterId, ClientRequestInfo requestInfo) {
        if (moderationRepository.findPending(ModerationTargetType.REVIEW.name(), reviewId).isPresent()) {
            throw new ConflictException("This review already has a pending moderation case");
        }
        Instant now = clock.instant();
        Object target = target(ModerationTargetType.REVIEW, reviewId);
        ModerationRecordEntity record = moderationRepository.save(ModerationRecordEntity.pending(
                submitterId, ModerationTargetType.REVIEW, reviewId, null,
                targetContentMapper.snapshot(target), now
        ));
        notifyModerators(record, ModerationTargetType.REVIEW, reviewId, now, submitterId);
        auditService.recordDomain(submitterId, "MODERATION_SUBMITTED", "REVIEW", reviewId, requestInfo);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ModerationRecordResponse> queue(ModerationTargetType type, int page, int size) {
        requireModerator(currentUserService.requireUserId());
        Page<ModerationRecordEntity> records = moderationRepository.queue(
                ModerationStatus.PENDING, type == null ? null : type.name(), PageRequest.of(page, size)
        );
        Set<Long> submitterIds = new LinkedHashSet<>();
        records.forEach(record -> {
            if (record.getSubmittedByUserId() != null) submitterIds.add(record.getSubmittedByUserId());
        });
        Map<Long, String> submitterNames = new HashMap<>();
        userRepository.findAllById(submitterIds)
                .forEach(user -> submitterNames.put(user.getId(), user.getDisplayName()));
        return PageResponse.from(records.map(record -> response(
                record, submitterNames.get(record.getSubmittedByUserId())
        )));
    }

    @Transactional(readOnly = true)
    @Override
    public ModerationRecordResponse detail(Long caseId) {
        Long actorId = currentUserService.requireUserId();
        ModerationRecordEntity record = requireCase(caseId);
        if (!Objects.equals(record.getSubmittedByUserId(), actorId) && !isModerator(actorId)) {
            throw new AccessDeniedException("Moderation case is not accessible");
        }
        Object target = target(record.targetType(), record.targetId());
        ModerationTargetContentResponse targetContent = hasSnapshot(record)
                ? targetContentMapper.fromSnapshot(record.getTargetSnapshot())
                : targetContentMapper.map(target);
        return response(record, target, targetStrategies.require(record.targetType()).version(target), targetContent,
                submittedByDisplayName(record));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ModerationRecordResponse> history(
            ModerationTargetType type,
            Long targetId,
            int page,
            int size
    ) {
        Long actorId = currentUserService.requireUserId();
        Object target = target(type, targetId);
        if (!targetStrategies.require(type).isOwner(target, actorId) && !isModerator(actorId)) {
            throw new AccessDeniedException("Moderation history is not accessible");
        }
        Page<ModerationRecordEntity> records = moderationRepository.history(
                type.name(), targetId, PageRequest.of(page, size)
        );
        return PageResponse.from(records.map(record -> response(record, target)));
    }

    @Transactional
    @Override
    public ModerationRecordResponse approve(
            Long caseId,
            ModerationDecisionRequest request,
            ClientRequestInfo requestInfo
    ) {
        return decide(caseId, ModerationDecision.APPROVED, request, requestInfo);
    }

    @Transactional
    @Override
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
    @Override
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
        ModerationTargetStrategy targetStrategy = targetStrategies.require(record.targetType());
        requireVersion(targetStrategy, target, request.targetVersion());
        requirePending(targetStrategy, target);

        targetStrategy.cancel(target);
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
        ModerationTargetStrategy targetStrategy = targetStrategies.require(record.targetType());
        requireVersion(targetStrategy, target, request.targetVersion());
        requirePending(targetStrategy, target);

        Instant now = clock.instant();
        if (decision == ModerationDecision.APPROVED) targetStrategy.approve(target, now);
        else targetStrategy.reject(target);
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
        return targetStrategies.require(type).findLocked(id);
    }

    private Object target(ModerationTargetType type, Long id) {
        return targetStrategies.require(type).find(id);
    }

    private void requireOwner(ModerationTargetStrategy strategy, Object target, Long actorId) {
        if (!strategy.isOwner(target, actorId)) {
            throw new AccessDeniedException("Moderation target is not owned by this user");
        }
    }

    private void requirePending(ModerationTargetStrategy strategy, Object target) {
        if (!strategy.isPending(target)) {
            throw new ConflictException("Moderation target is not pending");
        }
    }

    private void requireVersion(ModerationTargetStrategy strategy, Object target, Integer expected) {
        if (!Objects.equals(strategy.version(target), expected)) {
            throw new ConflictException("Target was changed by another request; reload and try again");
        }
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
        return response(record, submittedByDisplayName(record));
    }

    private ModerationRecordResponse response(ModerationRecordEntity record, String submittedByDisplayName) {
        Object target = target(record.targetType(), record.targetId());
        return response(record, target, targetStrategies.require(record.targetType()).version(target), null,
                submittedByDisplayName);
    }

    private ModerationRecordResponse response(ModerationRecordEntity record, Object target) {
        return response(record, target, targetStrategies.require(record.targetType()).version(target));
    }

    private ModerationRecordResponse response(ModerationRecordEntity record, Object target, Integer targetVersion) {
        return response(record, target, targetVersion, null, submittedByDisplayName(record));
    }

    private ModerationRecordResponse response(
            ModerationRecordEntity record, Object target, Integer targetVersion,
            ModerationTargetContentResponse targetContent, String submittedByDisplayName
    ) {
        return new ModerationRecordResponse(
                record.getId(), record.targetType(), record.targetId(),
                targetStrategies.require(record.targetType()).title(target), targetVersion,
                record.getSubmittedByUserId(), submittedByDisplayName, record.getModeratorUserId(),
                record.getStatus(), record.getStatus() == ModerationStatus.PENDING
                        && targetStrategies.require(record.targetType()).isPending(target),
                record.getDecision(),
                record.getSubmissionNote(), record.getDecisionReason(), record.getSubmittedAt(), record.getResolvedAt(),
                hasSnapshot(record), targetContent
        );
    }

    private boolean hasSnapshot(ModerationRecordEntity record) {
        return record.getTargetSnapshot() != null && !record.getTargetSnapshot().isEmpty();
    }

    private String submittedByDisplayName(ModerationRecordEntity record) {
        return record.getSubmittedByUserId() == null ? null : userRepository.findById(record.getSubmittedByUserId())
                .map(user -> user.getDisplayName()).orElse(null);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
