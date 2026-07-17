package com.ltss.service.community.impl;

import com.ltss.service.community.ReviewService;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.entity.auth.UserEntity;
import com.ltss.repository.auth.UserRepository;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.service.auth.AuditService;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.community.*;
import com.ltss.entity.community.*;
import com.ltss.repository.community.*;
import com.ltss.entity.content.*;
import com.ltss.repository.content.*;
import com.ltss.entity.moderation.NotificationEntity;
import com.ltss.repository.moderation.NotificationRepository;
import com.ltss.service.moderation.ModerationService;
import com.ltss.entity.place.PlaceStatus;
import com.ltss.repository.place.PlaceRepository;
import com.ltss.entity.tour.*;
import com.ltss.repository.tour.TourRepository;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository replyRepository;
    private final ReviewMediaRepository mediaRepository;
    private final ProhibitedTermRepository prohibitedTermRepository;
    private final PlaceRepository placeRepository;
    private final BusinessRepository businessRepository;
    private final ArticleRepository articleRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final ModerationService moderationService;
    private final AuditService auditService;
    private final Clock clock;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ReviewReplyRepository replyRepository,
                         ReviewMediaRepository mediaRepository, ProhibitedTermRepository prohibitedTermRepository,
                         PlaceRepository placeRepository, BusinessRepository businessRepository,
                         ArticleRepository articleRepository, TourRepository tourRepository,
                         UserRepository userRepository, NotificationRepository notificationRepository,
                         CurrentUserService currentUserService, ModerationService moderationService,
                         AuditService auditService, Clock clock) {
        this.reviewRepository = reviewRepository;
        this.replyRepository = replyRepository;
        this.mediaRepository = mediaRepository;
        this.prohibitedTermRepository = prohibitedTermRepository;
        this.placeRepository = placeRepository;
        this.businessRepository = businessRepository;
        this.articleRepository = articleRepository;
        this.tourRepository = tourRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.moderationService = moderationService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    @Override
    public ReviewResponse create(ReviewTargetType type, Long targetId, CreateReviewRequest request,
                                 ClientRequestInfo requestInfo) {
        Long userId = currentUserService.requireUserId();
        validatePublicTarget(type, targetId);
        if (reviewRepository.findExisting(userId, type.name(), targetId).isPresent()) {
            throw new ConflictException("You have already reviewed this target");
        }
        String comment = request.comment().trim();
        if (comment.length() < 20) throw new ConflictException("Review comment must contain at least 20 characters");
        rejectProhibitedTerms(comment);
        List<Long> mediaIds = validateMedia(request.mediaAssetIds());
        Instant now = clock.instant();
        ReviewEntity review = reviewRepository.save(new ReviewEntity(
                userId, type, targetId, request.rating(), comment, now
        ));
        for (int index = 0; index < mediaIds.size(); index++) {
            mediaRepository.save(new ReviewMediaEntity(review.getId(), mediaIds.get(index), index + 1));
        }
        moderationService.registerPendingReview(review.getId(), userId, requestInfo);
        return response(review, context(List.of(review)));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ReviewResponse> visible(ReviewTargetType type, Long targetId, int page, int size) {
        validatePublicTarget(type, targetId);
        Page<ReviewEntity> reviews = reviewRepository.findVisible(type.name(), targetId, PageRequest.of(page, size));
        ReviewContext context = context(reviews.getContent());
        return PageResponse.from(reviews.map(review -> response(review, context)));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ReviewResponse> mine(int page, int size) {
        Page<ReviewEntity> reviews = reviewRepository.findAllByUserIdOrderBySubmittedAtDesc(
                currentUserService.requireUserId(), PageRequest.of(page, size)
        );
        ReviewContext context = context(reviews.getContent());
        return PageResponse.from(reviews.map(review -> response(review, context)));
    }

    @Transactional
    @Override
    public ReviewResponse reply(Long reviewId, ReviewReplyRequest request, ClientRequestInfo requestInfo) {
        Long actorId = currentUserService.requireUserId();
        ReviewEntity review = reviewRepository.findLockedById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review was not found"));
        if (review.getStatus() != ReviewStatus.VISIBLE || review.getBusinessId() == null) {
            throw new ConflictException("Only a visible business review can receive an official reply");
        }
        BusinessEntity business = businessRepository.findById(review.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business was not found"));
        if (!Objects.equals(business.getOwnerUserId(), actorId)) {
            throw new AccessDeniedException("Only the reviewed business owner can reply");
        }
        if (replyRepository.findByReviewId(reviewId).isPresent()) {
            throw new ConflictException("This review already has an official reply");
        }
        String content = request.content().trim();
        rejectProhibitedTerms(content);
        replyRepository.save(new ReviewReplyEntity(reviewId, actorId, content));
        notificationRepository.save(new NotificationEntity(
                review.getUserId(), "Doanh nghiệp đã phản hồi đánh giá",
                "Đánh giá #" + reviewId + " vừa nhận được phản hồi chính thức.",
                "/businesses/" + business.getId(), clock.instant()
        ));
        auditService.recordDomain(actorId, "REVIEW_REPLIED", "REVIEW", reviewId, requestInfo);
        return response(review, context(List.of(review)));
    }

    private void validatePublicTarget(ReviewTargetType type, Long id) {
        boolean valid = switch (type) {
            case PLACE -> placeRepository.findById(id).map(place -> place.getStatus() == PlaceStatus.PUBLISHED).orElse(false);
            case BUSINESS -> businessRepository.findById(id).map(business ->
                    business.getStatus() == BusinessStatus.ACTIVE && placeRepository.findById(business.getPlaceId())
                            .map(place -> place.getStatus() == PlaceStatus.PUBLISHED).orElse(false)).orElse(false);
            case ARTICLE -> articleRepository.findById(id).map(article -> article.getStatus() == PublicationStatus.PUBLISHED).orElse(false);
            case TOUR -> tourRepository.findById(id).map(tour -> tour.getStatus() == TourStatus.PUBLISHED
                    && tour.getVisibility() != TourVisibility.PRIVATE).orElse(false);
        };
        if (!valid) throw new ResourceNotFoundException("Review target was not found");
    }

    private List<Long> validateMedia(List<Long> requested) {
        if (requested == null || requested.isEmpty()) return List.of();
        LinkedHashSet<Long> unique = new LinkedHashSet<>(requested);
        if (unique.size() != requested.size()) throw new ConflictException("Review images must be unique");
        List<Long> valid = mediaRepository.findValidImageIds(unique);
        if (valid.size() != unique.size()) throw new ConflictException("Every review attachment must be an active image");
        return requested;
    }

    private void rejectProhibitedTerms(String content) {
        String normalized = normalizeText(content);
        boolean blocked = prohibitedTermRepository.findAllByActiveTrueAndSeverityOrderByIdAsc("BLOCK").stream()
                .map(ProhibitedTermEntity::getNormalizedTerm)
                .filter(Objects::nonNull)
                .anyMatch(normalized::contains);
        if (blocked) throw new ConflictException("Content contains a prohibited term");
    }

    private ReviewContext context(Collection<ReviewEntity> reviews) {
        if (reviews.isEmpty()) return new ReviewContext(Map.of(), Map.of(), Map.of());
        List<Long> reviewIds = reviews.stream().map(ReviewEntity::getId).toList();
        Set<Long> userIds = reviews.stream().map(ReviewEntity::getUserId).collect(Collectors.toSet());
        List<ReviewReplyEntity> replies = replyRepository.findAllByReviewIdIn(reviewIds);
        replies.stream().map(ReviewReplyEntity::getRepliedByUserId).filter(Objects::nonNull).forEach(userIds::add);
        Map<Long, UserEntity> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        Map<Long, ReviewReplyEntity> repliesByReview = replies.stream()
                .collect(Collectors.toMap(ReviewReplyEntity::getReviewId, Function.identity()));
        Map<Long, List<ReviewMediaResponse>> media = mediaRepository.findMedia(reviewIds).stream()
                .collect(Collectors.groupingBy(ReviewMediaProjection::getReviewId, LinkedHashMap::new,
                        Collectors.mapping(item -> new ReviewMediaResponse(item.getMediaId(), item.getMediaUrl(),
                                item.getThumbnailUrl(), item.getDisplayOrder()), Collectors.toList())));
        return new ReviewContext(users, repliesByReview, media);
    }

    private ReviewResponse response(ReviewEntity review, ReviewContext context) {
        UserEntity user = context.users().get(review.getUserId());
        ReviewReplyEntity reply = context.replies().get(review.getId());
        ReviewReplyResponse replyResponse = null;
        if (reply != null) {
            UserEntity replier = context.users().get(reply.getRepliedByUserId());
            replyResponse = new ReviewReplyResponse(reply.getId(), reply.getRepliedByUserId(),
                    replier == null ? "Doanh nghiệp" : replier.getDisplayName(), reply.getContent(),
                    reply.getVersion(), reply.getCreatedAt());
        }
        return new ReviewResponse(review.getId(), review.getUserId(),
                user == null ? "Người dùng LTSS" : user.getDisplayName(), user == null ? null : user.getAvatarUrl(),
                review.targetType(), review.targetId(), review.getRating(), review.getComment(), review.getStatus(),
                context.media().getOrDefault(review.getId(), List.of()), replyResponse, review.getVersion(),
                review.getSubmittedAt(), review.getPublishedAt());
    }

    private String normalizeText(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }

    private record ReviewContext(Map<Long, UserEntity> users,
                                 Map<Long, ReviewReplyEntity> replies,
                                 Map<Long, List<ReviewMediaResponse>> media) {}
}
