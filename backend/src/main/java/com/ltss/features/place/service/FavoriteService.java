package com.ltss.features.place.service;

import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.place.dto.FavoriteStateResponse;
import com.ltss.features.place.dto.PlaceSummaryResponse;
import com.ltss.features.place.entity.PlaceEntity;
import com.ltss.features.place.entity.PlaceStatus;
import com.ltss.features.place.repository.FavoriteRepository;
import com.ltss.features.place.repository.PlaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final PlaceRepository placeRepository;
    private final DiscoveryService discoveryService;
    private final CurrentUserService currentUserService;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            PlaceRepository placeRepository,
            DiscoveryService discoveryService,
            CurrentUserService currentUserService
    ) {
        this.favoriteRepository = favoriteRepository;
        this.placeRepository = placeRepository;
        this.discoveryService = discoveryService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public FavoriteStateResponse add(Long placeId) {
        requirePublished(placeId);
        Long userId = currentUserService.requireUserId();
        favoriteRepository.addIfAbsent(userId, placeId);
        return new FavoriteStateResponse(placeId, true);
    }

    @Transactional
    public FavoriteStateResponse remove(Long placeId) {
        favoriteRepository.deleteOwned(currentUserService.requireUserId(), placeId);
        return new FavoriteStateResponse(placeId, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaceSummaryResponse> listMine(int page, int size) {
        Long userId = currentUserService.requireUserId();
        Page<Long> ids = favoriteRepository.findPublishedPlaceIds(userId, PageRequest.of(page, size));
        List<PlaceEntity> places = ids.isEmpty()
                ? List.of()
                : placeRepository.findAllByIdInAndStatus(ids.getContent(), PlaceStatus.PUBLISHED);
        Map<Long, PlaceEntity> byId = places.stream()
                .collect(Collectors.toMap(PlaceEntity::getId, Function.identity()));
        DiscoveryService.Enrichment enrichment = discoveryService.enrichment(ids.getContent(), userId);
        List<PlaceSummaryResponse> content = ids.getContent().stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(place -> discoveryService.summary(place, null, enrichment))
                .toList();
        return PageResponse.from(new PageImpl<>(content, ids.getPageable(), ids.getTotalElements()));
    }

    private void requirePublished(Long placeId) {
        placeRepository.findById(placeId)
                .filter(place -> place.getStatus() == PlaceStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Place was not found"));
    }
}
