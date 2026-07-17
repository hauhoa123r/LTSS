package com.ltss.features.place.service;

import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.place.dto.HotspotResponse;
import com.ltss.features.place.dto.PlaceCategoryResponse;
import com.ltss.features.place.dto.PlaceDetailResponse;
import com.ltss.features.place.dto.PlaceMediaResponse;
import com.ltss.features.place.dto.PlaceSummaryResponse;
import com.ltss.features.place.dto.RelicDetailResponse;
import com.ltss.features.place.entity.PlaceCategoryEntity;
import com.ltss.features.place.entity.PlaceEntity;
import com.ltss.features.place.entity.PlaceStatus;
import com.ltss.features.place.repository.FavoriteRepository;
import com.ltss.features.place.repository.HotspotProjection;
import com.ltss.features.place.repository.NearbyPlaceProjection;
import com.ltss.features.place.repository.PlaceCategoryRepository;
import com.ltss.features.place.repository.PlaceMediaProjection;
import com.ltss.features.place.repository.PlaceMediaRepository;
import com.ltss.features.place.repository.PlaceRepository;
import com.ltss.features.place.repository.RelicDetailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DiscoveryService {
    private final PlaceRepository placeRepository;
    private final PlaceCategoryRepository categoryRepository;
    private final RelicDetailRepository relicDetailRepository;
    private final PlaceMediaRepository mediaRepository;
    private final FavoriteRepository favoriteRepository;
    private final CurrentUserService currentUserService;
    private final SearchHistoryService searchHistoryService;

    public DiscoveryService(
            PlaceRepository placeRepository,
            PlaceCategoryRepository categoryRepository,
            RelicDetailRepository relicDetailRepository,
            PlaceMediaRepository mediaRepository,
            FavoriteRepository favoriteRepository,
            CurrentUserService currentUserService,
            SearchHistoryService searchHistoryService
    ) {
        this.placeRepository = placeRepository;
        this.categoryRepository = categoryRepository;
        this.relicDetailRepository = relicDetailRepository;
        this.mediaRepository = mediaRepository;
        this.favoriteRepository = favoriteRepository;
        this.currentUserService = currentUserService;
        this.searchHistoryService = searchHistoryService;
    }

    @Transactional(readOnly = true)
    public List<PlaceCategoryResponse> categories() {
        return categoryRepository.findAllByActiveTrueOrderByCategoryNameAsc().stream()
                .map(PlaceCategoryResponse::from)
                .toList();
    }

    @Transactional
    public PageResponse<PlaceSummaryResponse> search(
            String rawQuery,
            String rawCategorySlug,
            int page,
            int size
    ) {
        String query = normalizeNullable(rawQuery);
        String categorySlug = normalizeNullable(rawCategorySlug);
        Long userId = currentUserService.optionalUserId();
        if (userId != null && query != null) {
            searchHistoryService.record(userId, query);
        }

        Page<PlaceEntity> places = placeRepository.searchPublished(
                query, categorySlug, PageRequest.of(page, size)
        );
        Enrichment enrichment = enrichment(
                places.getContent().stream().map(PlaceEntity::getId).toList(), userId
        );
        return PageResponse.from(places.map(place -> summary(place, null, enrichment)));
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaceSummaryResponse> nearby(
            double latitude,
            double longitude,
            double radiusKm,
            String rawCategorySlug,
            int page,
            int size
    ) {
        Page<NearbyPlaceProjection> places = placeRepository.findNearby(
                latitude,
                longitude,
                radiusKm,
                normalizeNullable(rawCategorySlug),
                PageRequest.of(page, size)
        );
        Long userId = currentUserService.optionalUserId();
        Enrichment enrichment = enrichment(
                places.getContent().stream().map(NearbyPlaceProjection::getPlaceId).toList(), userId
        );
        return PageResponse.from(places.map(place -> summary(place, enrichment)));
    }

    @Transactional(readOnly = true)
    public PlaceDetailResponse detail(String slug) {
        PlaceEntity place = placeRepository.findBySlugAndStatus(slug, PlaceStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Place was not found"));
        PlaceCategoryEntity category = categoryRepository.findById(place.getCategoryId())
                .filter(PlaceCategoryEntity::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Place was not found"));
        Long userId = currentUserService.optionalUserId();
        boolean favorite = userId != null && favoriteRepository.existsById(
                new com.ltss.features.place.entity.FavoriteId(userId, place.getId())
        );

        List<PlaceMediaProjection> media = mediaRepository.findMediaForPlaces(List.of(place.getId()));
        Map<Long, List<HotspotResponse>> hotspots = mediaRepository.findActiveHotspots(place.getId()).stream()
                .collect(Collectors.groupingBy(
                        HotspotProjection::getSourceMediaAssetId,
                        LinkedHashMap::new,
                        Collectors.mapping(HotspotResponse::from, Collectors.toList())
                ));
        List<PlaceMediaResponse> mediaResponses = media.stream()
                .map(item -> media(item, hotspots.getOrDefault(item.getMediaId(), List.of())))
                .toList();

        return new PlaceDetailResponse(
                place.getId(),
                place.getName(),
                place.getSlug(),
                place.getSummary(),
                place.getDescription(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getOpeningHours(),
                place.getEntranceFee(),
                place.getContactPhone(),
                PlaceCategoryResponse.from(category),
                relicDetailRepository.findById(place.getId()).map(RelicDetailResponse::from).orElse(null),
                mediaResponses,
                favorite,
                place.getVersion()
        );
    }

    Enrichment enrichment(Collection<Long> placeIds, Long userId) {
        if (placeIds.isEmpty()) {
            return new Enrichment(Map.of(), Map.of(), Set.of());
        }
        Map<Long, PlaceCategoryResponse> categories = categoryRepository.findAllById(
                        placeRepository.findAllById(placeIds).stream().map(PlaceEntity::getCategoryId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(PlaceCategoryEntity::getId, PlaceCategoryResponse::from));
        Map<Long, String> covers = new HashMap<>();
        for (PlaceMediaProjection media : mediaRepository.findMediaForPlaces(placeIds)) {
            if (!"IMAGE".equals(media.getMediaType()) && !"PANORAMA_360".equals(media.getMediaType())) {
                continue;
            }
            covers.putIfAbsent(
                    media.getPlaceId(),
                    media.getThumbnailUrl() == null ? media.getMediaUrl() : media.getThumbnailUrl()
            );
        }
        Set<Long> favorites = userId == null
                ? Set.of()
                : new HashSet<>(favoriteRepository.findFavoritePlaceIds(userId, placeIds));
        return new Enrichment(categories, covers, favorites);
    }

    PlaceSummaryResponse summary(PlaceEntity place, Double distance, Enrichment enrichment) {
        return new PlaceSummaryResponse(
                place.getId(), place.getName(), place.getSlug(), place.getSummary(), place.getAddress(),
                place.getLatitude(), place.getLongitude(), place.getEntranceFee(),
                enrichment.categories().get(place.getCategoryId()),
                enrichment.covers().get(place.getId()), distance,
                enrichment.favorites().contains(place.getId())
        );
    }

    private PlaceSummaryResponse summary(NearbyPlaceProjection place, Enrichment enrichment) {
        return new PlaceSummaryResponse(
                place.getPlaceId(), place.getName(), place.getSlug(), place.getSummary(), place.getAddress(),
                place.getLatitude(), place.getLongitude(), place.getEntranceFee(),
                enrichment.categories().get(place.getCategoryId()),
                enrichment.covers().get(place.getPlaceId()), place.getDistanceKm(),
                enrichment.favorites().contains(place.getPlaceId())
        );
    }

    private PlaceMediaResponse media(PlaceMediaProjection media, List<HotspotResponse> hotspots) {
        return new PlaceMediaResponse(
                media.getMediaId(), media.getMediaType(), media.getMediaUrl(), media.getThumbnailUrl(),
                media.getMimeType(), media.getFileSizeBytes(), media.getWidthPx(), media.getHeightPx(),
                media.getDurationSeconds(), media.getUsageType(), media.getDisplayOrder(),
                Boolean.TRUE.equals(media.getPrimaryMedia()), hotspots
        );
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    record Enrichment(
            Map<Long, PlaceCategoryResponse> categories,
            Map<Long, String> covers,
            Set<Long> favorites
    ) {
    }
}
