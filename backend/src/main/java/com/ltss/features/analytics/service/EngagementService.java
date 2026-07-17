package com.ltss.features.analytics.service;

import com.ltss.common.exception.*;
import com.ltss.features.analytics.dto.*;
import com.ltss.features.analytics.entity.*;
import com.ltss.features.analytics.repository.*;
import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.content.entity.*;
import com.ltss.features.content.repository.*;
import com.ltss.features.place.entity.PlaceStatus;
import com.ltss.features.place.repository.PlaceRepository;
import com.ltss.features.tour.entity.*;
import com.ltss.features.tour.repository.TourRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class EngagementService {
    private static final int MAX_EVENTS_PER_MINUTE = 120;
    private static final Pattern SAFE_METADATA_KEY = Pattern.compile("[a-zA-Z][a-zA-Z0-9_.-]{0,49}");
    private static final Set<String> SENSITIVE_FRAGMENTS = Set.of(
            "password", "token", "secret", "authorization", "cookie", "otp", "hash"
    );

    private final EngagementEventTypeRepository typeRepository;
    private final EngagementEventRepository eventRepository;
    private final PlaceRepository placeRepository;
    private final BusinessRepository businessRepository;
    private final EventRepository contentEventRepository;
    private final ArticleRepository articleRepository;
    private final BusinessPostRepository postRepository;
    private final PromotionRepository promotionRepository;
    private final TourRepository tourRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public EngagementService(
            EngagementEventTypeRepository typeRepository, EngagementEventRepository eventRepository,
            PlaceRepository placeRepository, BusinessRepository businessRepository,
            EventRepository contentEventRepository, ArticleRepository articleRepository,
            BusinessPostRepository postRepository, PromotionRepository promotionRepository,
            TourRepository tourRepository, CurrentUserService currentUserService, Clock clock
    ) {
        this.typeRepository = typeRepository;
        this.eventRepository = eventRepository;
        this.placeRepository = placeRepository;
        this.businessRepository = businessRepository;
        this.contentEventRepository = contentEventRepository;
        this.articleRepository = articleRepository;
        this.postRepository = postRepository;
        this.promotionRepository = promotionRepository;
        this.tourRepository = tourRepository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional
    public EngagementAcceptedResponse record(EngagementEventRequest request) {
        String code = request.eventTypeCode().trim();
        EngagementEventTypeEntity type = typeRepository.findLockedByCode(code)
                .filter(EngagementEventTypeEntity::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement event type is not active"));
        validateTarget(request.targetType(), request.targetId());
        Map<String, String> metadata = validateMetadata(request.metadata());
        String sessionKey = request.sessionKey().trim();
        Instant now = clock.instant();
        if (eventRepository.countBySessionKeyAndOccurredAtGreaterThanEqual(sessionKey, now.minusSeconds(60))
                >= MAX_EVENTS_PER_MINUTE) {
            throw new RateLimitException("Too many engagement events for this session");
        }
        if ("VIEW".equals(code) && eventRepository.countDuplicate(
                code, sessionKey, request.targetType().name(), request.targetId(), now.minus(Duration.ofHours(24))
        ) > 0) {
            return new EngagementAcceptedResponse(false);
        }
        eventRepository.save(new EngagementEventEntity(
                code, currentUserService.optionalUserId(), sessionKey,
                request.targetType(), request.targetId(), metadata, now
        ));
        return new EngagementAcceptedResponse(true);
    }

    private void validateTarget(EngagementTargetType type, Long id) {
        boolean available = switch (type) {
            case PLACE -> placeRepository.findById(id).map(item -> item.getStatus() == PlaceStatus.PUBLISHED).orElse(false);
            case BUSINESS -> businessRepository.findById(id).map(this::isPublicBusiness).orElse(false);
            case EVENT -> contentEventRepository.findById(id).map(item -> item.getStatus() == EventStatus.PUBLISHED).orElse(false);
            case ARTICLE -> articleRepository.findById(id).map(item -> item.getStatus() == PublicationStatus.PUBLISHED).orElse(false);
            case BUSINESS_POST -> postRepository.findById(id).map(item -> item.getStatus() == PublicationStatus.PUBLISHED
                    && businessRepository.findById(item.getBusinessId()).map(this::isPublicBusiness).orElse(false)).orElse(false);
            case PROMOTION -> promotionRepository.findById(id).map(item -> {
                Instant now = clock.instant();
                return item.getStatus() == PromotionStatus.ACTIVE && !item.getStartAt().isAfter(now)
                        && item.getEndAt().isAfter(now)
                        && businessRepository.findById(item.getBusinessId()).map(this::isPublicBusiness).orElse(false);
            }).orElse(false);
            case TOUR -> tourRepository.findById(id).map(item -> item.getStatus() == TourStatus.PUBLISHED
                    && item.getVisibility() != TourVisibility.PRIVATE).orElse(false);
        };
        if (!available) throw new ResourceNotFoundException("Engagement target was not found");
    }

    private boolean isPublicBusiness(BusinessEntity business) {
        return business.getStatus() == BusinessStatus.ACTIVE
                && placeRepository.findById(business.getPlaceId())
                .map(place -> place.getStatus() == PlaceStatus.PUBLISHED).orElse(false);
    }

    private Map<String, String> validateMetadata(Map<String, String> input) {
        if (input == null || input.isEmpty()) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            String normalizedKey = key == null ? "" : key.trim();
            String lower = normalizedKey.toLowerCase(Locale.ROOT);
            if (!SAFE_METADATA_KEY.matcher(normalizedKey).matches()
                    || SENSITIVE_FRAGMENTS.stream().anyMatch(lower::contains)) {
                throw new BusinessRuleViolationException("Engagement metadata contains an unsupported key");
            }
            if (value == null) throw new BusinessRuleViolationException("Engagement metadata values cannot be null");
            result.put(normalizedKey, value.trim());
        });
        return result;
    }
}
