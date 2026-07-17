package com.ltss.features.content.service;

import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.features.content.dto.*;
import com.ltss.features.content.entity.*;
import com.ltss.features.content.repository.*;
import com.ltss.features.place.entity.PlaceEntity;
import com.ltss.features.place.entity.PlaceStatus;
import com.ltss.features.place.repository.PlaceMediaProjection;
import com.ltss.features.place.repository.PlaceMediaRepository;
import com.ltss.features.place.repository.PlaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BusinessPublicService {
    private final BusinessRepository businessRepository;
    private final BusinessPostRepository postRepository;
    private final PromotionRepository promotionRepository;
    private final PlaceRepository placeRepository;
    private final PlaceMediaRepository placeMediaRepository;
    private final ContentMediaRepository mediaRepository;
    private final Clock clock;

    public BusinessPublicService(
            BusinessRepository businessRepository,
            BusinessPostRepository postRepository,
            PromotionRepository promotionRepository,
            PlaceRepository placeRepository,
            PlaceMediaRepository placeMediaRepository,
            ContentMediaRepository mediaRepository,
            Clock clock
    ) {
        this.businessRepository = businessRepository;
        this.postRepository = postRepository;
        this.promotionRepository = promotionRepository;
        this.placeRepository = placeRepository;
        this.placeMediaRepository = placeMediaRepository;
        this.mediaRepository = mediaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<BusinessResponse> businesses(String query, int page, int size) {
        Page<BusinessEntity> businesses = businessRepository.searchActive(
                normalize(query), PageRequest.of(page, size)
        );
        BusinessContext context = businessContext(businesses.getContent());
        return PageResponse.from(businesses.map(business -> business(business, context)));
    }

    @Transactional(readOnly = true)
    public BusinessResponse business(Long id) {
        BusinessEntity business = requirePublicBusiness(id);
        return business(business, businessContext(List.of(business)));
    }

    @Transactional(readOnly = true)
    public PageResponse<BusinessPostSummaryResponse> posts(
            String query,
            Long businessId,
            int page,
            int size
    ) {
        Page<BusinessPostEntity> posts = postRepository.searchPublished(
                normalize(query), businessId, PageRequest.of(page, size)
        );
        PostContext context = postContext(posts.getContent());
        return PageResponse.from(posts.map(post -> postSummary(post, context)));
    }

    @Transactional(readOnly = true)
    public BusinessPostDetailResponse post(String slug) {
        BusinessPostEntity post = postRepository.findBySlugAndStatus(slug, PublicationStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Business post was not found"));
        BusinessEntity business = requirePublicBusiness(post.getBusinessId());
        PostContext context = postContext(List.of(post));
        return new BusinessPostDetailResponse(
                post.getId(), post.getTitle(), post.getSlug(), post.getSummary(), post.getContent(),
                business(business, businessContext(List.of(business))),
                context.tags().getOrDefault(post.getId(), List.of()),
                context.media().getOrDefault(post.getId(), List.of()),
                post.getPublishedAt(), post.getVersion()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> promotions(Long businessId, int page, int size) {
        Page<PromotionEntity> promotions = promotionRepository.findCurrent(
                businessId, clock.instant(), PageRequest.of(page, size)
        );
        PromotionContext context = promotionContext(promotions.getContent());
        return PageResponse.from(promotions.map(promotion -> promotion(promotion, context)));
    }

    @Transactional(readOnly = true)
    public PromotionResponse promotion(Long id) {
        PromotionEntity promotion = promotionRepository.findCurrentById(id, clock.instant())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion was not found"));
        return promotion(promotion, promotionContext(List.of(promotion)));
    }

    private BusinessEntity requirePublicBusiness(Long id) {
        BusinessEntity business = businessRepository.findByIdAndStatus(id, BusinessStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Business was not found"));
        placeRepository.findById(business.getPlaceId())
                .filter(place -> place.getStatus() == PlaceStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Business was not found"));
        return business;
    }

    private BusinessContext businessContext(Collection<BusinessEntity> businesses) {
        if (businesses.isEmpty()) return new BusinessContext(Map.of(), Map.of());
        Set<Long> placeIds = businesses.stream().map(BusinessEntity::getPlaceId).collect(Collectors.toSet());
        Map<Long, PlaceEntity> places = placeRepository.findAllById(placeIds).stream()
                .filter(place -> place.getStatus() == PlaceStatus.PUBLISHED)
                .collect(Collectors.toMap(PlaceEntity::getId, Function.identity()));
        Map<Long, String> covers = coverUrls(placeMediaRepository.findMediaForPlaces(placeIds));
        return new BusinessContext(places, covers);
    }

    private BusinessResponse business(BusinessEntity business, BusinessContext context) {
        PlaceEntity place = context.places().get(business.getPlaceId());
        if (place == null) throw new ResourceNotFoundException("Business was not found");
        return new BusinessResponse(
                business.getId(), business.getRegistrationNumber(), business.getContactEmail(),
                business.getWebsiteUrl(), LinkedPlaceResponse.from(place),
                context.covers().get(place.getId()), business.getVersion()
        );
    }

    private PostContext postContext(Collection<BusinessPostEntity> posts) {
        if (posts.isEmpty()) return new PostContext(Map.of(), Map.of(), Map.of(), Map.of());
        Set<Long> businessIds = posts.stream().map(BusinessPostEntity::getBusinessId).collect(Collectors.toSet());
        List<BusinessEntity> businesses = businessRepository.findAllById(businessIds).stream()
                .filter(item -> item.getStatus() == BusinessStatus.ACTIVE).toList();
        Map<Long, BusinessEntity> byId = businesses.stream()
                .collect(Collectors.toMap(BusinessEntity::getId, Function.identity()));
        BusinessContext businessesContext = businessContext(businesses);
        List<Long> postIds = posts.stream().map(BusinessPostEntity::getId).toList();
        return new PostContext(
                byId,
                businessesContext.places(),
                mediaMap(mediaRepository.postMedia(postIds)),
                tagMap(mediaRepository.postTags(postIds))
        );
    }

    private BusinessPostSummaryResponse postSummary(BusinessPostEntity post, PostContext context) {
        BusinessEntity business = context.businesses().get(post.getBusinessId());
        PlaceEntity place = business == null ? null : context.places().get(business.getPlaceId());
        return new BusinessPostSummaryResponse(
                post.getId(), post.getTitle(), post.getSlug(), post.getSummary(), post.getBusinessId(),
                place == null ? null : place.getName(), cover(context.media().get(post.getId())),
                context.tags().getOrDefault(post.getId(), List.of()), post.getPublishedAt()
        );
    }

    private PromotionContext promotionContext(Collection<PromotionEntity> promotions) {
        if (promotions.isEmpty()) return new PromotionContext(Map.of(), Map.of(), Map.of());
        Set<Long> businessIds = promotions.stream().map(PromotionEntity::getBusinessId).collect(Collectors.toSet());
        List<BusinessEntity> businesses = businessRepository.findAllById(businessIds).stream()
                .filter(item -> item.getStatus() == BusinessStatus.ACTIVE).toList();
        Map<Long, BusinessEntity> byId = businesses.stream()
                .collect(Collectors.toMap(BusinessEntity::getId, Function.identity()));
        Map<Long, PlaceEntity> places = businessContext(businesses).places();
        List<Long> promotionIds = promotions.stream().map(PromotionEntity::getId).toList();
        return new PromotionContext(byId, places, mediaMap(mediaRepository.promotionMedia(promotionIds)));
    }

    private PromotionResponse promotion(PromotionEntity promotion, PromotionContext context) {
        BusinessEntity business = context.businesses().get(promotion.getBusinessId());
        PlaceEntity place = business == null ? null : context.places().get(business.getPlaceId());
        List<ContentMediaResponse> media = context.media().getOrDefault(promotion.getId(), List.of());
        return new PromotionResponse(
                promotion.getId(), promotion.getTitle(), promotion.getDescription(), promotion.getDiscountType(),
                promotion.getDiscountValue(), promotion.getPromoCode(), promotion.getStartAt(), promotion.getEndAt(),
                promotion.getBusinessId(), place == null ? null : place.getName(), cover(media), media,
                promotion.getVersion()
        );
    }

    private Map<Long, List<ContentMediaResponse>> mediaMap(List<TargetMediaProjection> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                TargetMediaProjection::getTargetId,
                LinkedHashMap::new,
                Collectors.mapping(ContentMediaResponse::from, Collectors.toList())
        ));
    }

    private Map<Long, List<TagResponse>> tagMap(List<TagProjection> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                TagProjection::getPostId,
                LinkedHashMap::new,
                Collectors.mapping(tag -> new TagResponse(tag.getName(), tag.getSlug()), Collectors.toList())
        ));
    }

    private Map<Long, String> coverUrls(List<PlaceMediaProjection> rows) {
        Map<Long, String> covers = new HashMap<>();
        rows.stream()
                .filter(row -> "IMAGE".equals(row.getMediaType()) || "PANORAMA_360".equals(row.getMediaType()))
                .forEach(row -> covers.putIfAbsent(
                        row.getPlaceId(), row.getThumbnailUrl() == null ? row.getMediaUrl() : row.getThumbnailUrl()
                ));
        return covers;
    }

    private String cover(List<ContentMediaResponse> media) {
        if (media == null) return null;
        return media.stream()
                .filter(item -> "IMAGE".equals(item.mediaType()) || "PANORAMA_360".equals(item.mediaType()))
                .findFirst()
                .map(item -> item.thumbnailUrl() == null ? item.mediaUrl() : item.thumbnailUrl())
                .orElse(null);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record BusinessContext(Map<Long, PlaceEntity> places, Map<Long, String> covers) {}
    private record PostContext(
            Map<Long, BusinessEntity> businesses,
            Map<Long, PlaceEntity> places,
            Map<Long, List<ContentMediaResponse>> media,
            Map<Long, List<TagResponse>> tags
    ) {}
    private record PromotionContext(
            Map<Long, BusinessEntity> businesses,
            Map<Long, PlaceEntity> places,
            Map<Long, List<ContentMediaResponse>> media
    ) {}
}
