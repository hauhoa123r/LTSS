package com.ltss.features.content.service;

import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.features.content.dto.ArticleCategoryResponse;
import com.ltss.features.content.dto.ArticleDetailResponse;
import com.ltss.features.content.dto.ArticleSummaryResponse;
import com.ltss.features.content.dto.ContentMediaResponse;
import com.ltss.features.content.dto.EventDetailResponse;
import com.ltss.features.content.dto.EventSummaryResponse;
import com.ltss.features.content.dto.LinkedPlaceResponse;
import com.ltss.features.content.entity.ArticleCategoryEntity;
import com.ltss.features.content.entity.ArticleEntity;
import com.ltss.features.content.entity.EventEntity;
import com.ltss.features.content.entity.EventStatus;
import com.ltss.features.content.entity.PublicationStatus;
import com.ltss.features.content.repository.ArticleCategoryRepository;
import com.ltss.features.content.repository.ArticleRepository;
import com.ltss.features.content.repository.ContentMediaRepository;
import com.ltss.features.content.repository.EventRepository;
import com.ltss.features.content.repository.TargetMediaProjection;
import com.ltss.features.place.entity.PlaceEntity;
import com.ltss.features.place.entity.PlaceStatus;
import com.ltss.features.place.repository.PlaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EditorialPublicService {
    private final ArticleCategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final EventRepository eventRepository;
    private final PlaceRepository placeRepository;
    private final ContentMediaRepository mediaRepository;
    private final Clock clock;

    public EditorialPublicService(
            ArticleCategoryRepository categoryRepository,
            ArticleRepository articleRepository,
            EventRepository eventRepository,
            PlaceRepository placeRepository,
            ContentMediaRepository mediaRepository,
            Clock clock
    ) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
        this.eventRepository = eventRepository;
        this.placeRepository = placeRepository;
        this.mediaRepository = mediaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ArticleCategoryResponse> categories() {
        return categoryRepository.findAllByActiveTrueOrderByCategoryNameAsc().stream()
                .map(this::category)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleSummaryResponse> articles(String query, String categorySlug, int page, int size) {
        Page<ArticleEntity> articles = articleRepository.searchPublished(
                normalize(query), normalize(categorySlug), PageRequest.of(page, size)
        );
        ArticleContext context = articleContext(articles.getContent());
        return PageResponse.from(articles.map(article -> articleSummary(article, context)));
    }

    @Transactional(readOnly = true)
    public ArticleDetailResponse article(String slug) {
        ArticleEntity article = articleRepository.findBySlugAndStatus(slug, PublicationStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Article was not found"));
        ArticleContext context = articleContext(List.of(article));
        ArticleCategoryEntity category = context.categories().get(article.getCategoryId());
        if (category == null) throw new ResourceNotFoundException("Article was not found");
        return new ArticleDetailResponse(
                article.getId(), article.getTitle(), article.getSlug(), article.getSummary(), article.getContent(),
                category(category), linkedPlace(article.getPlaceId(), context.places()), article.getEventId(),
                context.media().getOrDefault(article.getId(), List.of()), article.getPublishedAt(), article.getVersion()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<EventSummaryResponse> events(String query, int page, int size) {
        Page<EventEntity> events = eventRepository.findUpcoming(
                normalize(query), clock.instant(), PageRequest.of(page, size)
        );
        EventContext context = eventContext(events.getContent());
        return PageResponse.from(events.map(event -> eventSummary(event, context)));
    }

    @Transactional(readOnly = true)
    public EventDetailResponse event(String slug) {
        EventEntity event = eventRepository.findBySlugAndStatus(slug, EventStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Event was not found"));
        EventContext context = eventContext(List.of(event));
        return new EventDetailResponse(
                event.getId(), event.getTitle(), event.getSlug(), event.getDescription(), event.getStartAt(),
                event.getEndAt(), event.getLocationNote(), linkedPlace(event.getPlaceId(), context.places()),
                context.media().getOrDefault(event.getId(), List.of()), event.getVersion()
        );
    }

    private ArticleContext articleContext(Collection<ArticleEntity> articles) {
        if (articles.isEmpty()) return new ArticleContext(Map.of(), Map.of(), Map.of());
        Set<Long> categoryIds = articles.stream().map(ArticleEntity::getCategoryId).collect(Collectors.toSet());
        Map<Long, ArticleCategoryEntity> categories = categoryRepository.findAllById(categoryIds).stream()
                .filter(ArticleCategoryEntity::isActive)
                .collect(Collectors.toMap(ArticleCategoryEntity::getId, Function.identity()));
        Map<Long, PlaceEntity> places = publicPlaces(
                articles.stream().map(ArticleEntity::getPlaceId).filter(java.util.Objects::nonNull).toList()
        );
        List<Long> articleIds = articles.stream().map(ArticleEntity::getId).toList();
        return new ArticleContext(categories, places, mediaMap(mediaRepository.articleMedia(articleIds)));
    }

    private EventContext eventContext(Collection<EventEntity> events) {
        if (events.isEmpty()) return new EventContext(Map.of(), Map.of());
        Map<Long, PlaceEntity> places = publicPlaces(
                events.stream().map(EventEntity::getPlaceId).filter(java.util.Objects::nonNull).toList()
        );
        List<Long> eventIds = events.stream().map(EventEntity::getId).toList();
        return new EventContext(places, mediaMap(mediaRepository.eventMedia(eventIds)));
    }

    private ArticleSummaryResponse articleSummary(ArticleEntity article, ArticleContext context) {
        ArticleCategoryEntity category = context.categories().get(article.getCategoryId());
        List<ContentMediaResponse> media = context.media().getOrDefault(article.getId(), List.of());
        return new ArticleSummaryResponse(
                article.getId(), article.getTitle(), article.getSlug(), article.getSummary(),
                category == null ? null : category(category), linkedPlace(article.getPlaceId(), context.places()),
                cover(media), article.getPublishedAt()
        );
    }

    private EventSummaryResponse eventSummary(EventEntity event, EventContext context) {
        List<ContentMediaResponse> media = context.media().getOrDefault(event.getId(), List.of());
        return new EventSummaryResponse(
                event.getId(), event.getTitle(), event.getSlug(), event.getStartAt(), event.getEndAt(),
                event.getLocationNote(), linkedPlace(event.getPlaceId(), context.places()), cover(media)
        );
    }

    private Map<Long, PlaceEntity> publicPlaces(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return placeRepository.findAllByIdInAndStatus(Set.copyOf(ids), PlaceStatus.PUBLISHED).stream()
                .collect(Collectors.toMap(PlaceEntity::getId, Function.identity()));
    }

    private LinkedPlaceResponse linkedPlace(Long placeId, Map<Long, PlaceEntity> places) {
        if (placeId == null) return null;
        PlaceEntity place = places.get(placeId);
        return place == null ? null : LinkedPlaceResponse.from(place);
    }

    private ArticleCategoryResponse category(ArticleCategoryEntity category) {
        return new ArticleCategoryResponse(
                category.getId(), category.getCategoryName(), category.getSlug(), category.getDescription()
        );
    }

    private Map<Long, List<ContentMediaResponse>> mediaMap(List<TargetMediaProjection> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                TargetMediaProjection::getTargetId,
                LinkedHashMap::new,
                Collectors.mapping(ContentMediaResponse::from, Collectors.toList())
        ));
    }

    private String cover(List<ContentMediaResponse> media) {
        return media.stream()
                .filter(item -> "IMAGE".equals(item.mediaType()) || "PANORAMA_360".equals(item.mediaType()))
                .findFirst()
                .map(item -> item.thumbnailUrl() == null ? item.mediaUrl() : item.thumbnailUrl())
                .orElse(null);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ArticleContext(
            Map<Long, ArticleCategoryEntity> categories,
            Map<Long, PlaceEntity> places,
            Map<Long, List<ContentMediaResponse>> media
    ) {}

    private record EventContext(
            Map<Long, PlaceEntity> places,
            Map<Long, List<ContentMediaResponse>> media
    ) {}
}
