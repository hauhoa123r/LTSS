package com.ltss.features.tour.service;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.features.auth.entity.UserEntity;
import com.ltss.features.auth.repository.UserRepository;
import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.auth.service.AuditService;
import com.ltss.features.auth.service.ClientRequestInfo;
import com.ltss.features.place.entity.PlaceEntity;
import com.ltss.features.place.entity.PlaceStatus;
import com.ltss.features.place.repository.PlaceRepository;
import com.ltss.features.tour.dto.*;
import com.ltss.features.tour.entity.*;
import com.ltss.features.tour.repository.TourItemRepository;
import com.ltss.features.tour.repository.TourRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TourService {
    private final TourRepository tourRepository;
    private final TourItemRepository itemRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final Clock clock;

    public TourService(TourRepository tourRepository, TourItemRepository itemRepository,
                       PlaceRepository placeRepository, UserRepository userRepository,
                       CurrentUserService currentUserService, AuditService auditService, Clock clock) {
        this.tourRepository = tourRepository;
        this.itemRepository = itemRepository;
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public TourDetailResponse create(TourUpsertRequest request, ClientRequestInfo requestInfo) {
        Long ownerId = currentUserService.requireUserId();
        List<PlaceEntity> places = validateItems(request.items());
        TourEntity tour = tourRepository.save(new TourEntity(
                ownerId, null, normalize(request.title()), normalize(request.description()),
                normalize(request.region()), normalize(request.difficultyLevel()),
                request.estimatedDistanceKm(), request.estimatedDurationMinutes()
        ));
        saveItems(tour.getId(), request.items());
        auditService.recordDomain(ownerId, "TOUR_CREATED", "TOUR", tour.getId(), requestInfo);
        return detail(tour, places, true);
    }

    @Transactional
    public TourDetailResponse update(Long tourId, TourUpsertRequest request, ClientRequestInfo requestInfo) {
        Long ownerId = currentUserService.requireUserId();
        TourEntity tour = ownedLocked(tourId, ownerId);
        requireEditable(tour);
        if (request.version() == null) throw new ConflictException("Tour version is required for update");
        requireVersion(tour, request.version());
        List<PlaceEntity> places = validateItems(request.items());
        tour.update(normalize(request.title()), normalize(request.description()), normalize(request.region()),
                normalize(request.difficultyLevel()), request.estimatedDistanceKm(), request.estimatedDurationMinutes());
        itemRepository.deleteAllByTourId(tourId);
        saveItems(tourId, request.items());
        auditService.recordDomain(ownerId, "TOUR_UPDATED", "TOUR", tourId, requestInfo);
        return detail(tour, places, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<TourSummaryResponse> mine(int page, int size) {
        Page<TourEntity> tours = tourRepository.findAllByOwnerUserIdAndStatusNotOrderByUpdatedAtDesc(
                currentUserService.requireUserId(), TourStatus.DELETED, PageRequest.of(page, size)
        );
        return PageResponse.from(tours.map(this::summary));
    }

    @Transactional(readOnly = true)
    public PageResponse<TourSummaryResponse> publicTours(String query, int page, int size) {
        Page<TourEntity> tours = tourRepository.searchPublic(normalize(query), PageRequest.of(page, size));
        return PageResponse.from(tours.map(this::summary));
    }

    @Transactional(readOnly = true)
    public TourDetailResponse detail(Long tourId) {
        TourEntity tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour was not found"));
        Long userId = currentUserService.optionalUserId();
        boolean owner = Objects.equals(tour.getOwnerUserId(), userId);
        boolean shared = tour.getStatus() == TourStatus.PUBLISHED && tour.getVisibility() != TourVisibility.PRIVATE;
        if (!owner && !shared) throw new ResourceNotFoundException("Tour was not found");
        return detail(tour, null, owner);
    }

    @Transactional
    public TourDetailResponse copy(Long sourceId, ClientRequestInfo requestInfo) {
        Long ownerId = currentUserService.requireUserId();
        TourEntity source = tourRepository.findById(sourceId)
                .filter(tour -> tour.getStatus() == TourStatus.PUBLISHED)
                .filter(tour -> tour.getVisibility() != TourVisibility.PRIVATE || Objects.equals(tour.getOwnerUserId(), ownerId))
                .orElseThrow(() -> new ResourceNotFoundException("Source tour was not found"));
        List<TourItemEntity> sourceItems = itemRepository.findAllByTourIdOrderByVisitOrderAsc(sourceId);
        if (sourceItems.size() < 2 || sourceItems.size() > 10) {
            throw new ConflictException("Source tour does not have a valid stop count");
        }
        TourEntity copy = tourRepository.save(new TourEntity(
                ownerId, sourceId, copyTitle(source.getTitle()), source.getDescription(), source.getRegion(),
                source.getDifficultyLevel(), source.getEstimatedDistanceKm(), source.getEstimatedDurationMinutes()
        ));
        itemRepository.saveAll(sourceItems.stream().map(item -> new TourItemEntity(
                copy.getId(), item.getPlaceId(), item.getVisitOrder(), item.getPlannedStartAt(),
                item.getDurationMinutes(), item.getTransportMethod(), item.getNote()
        )).toList());
        auditService.recordDomain(ownerId, "TOUR_COPIED", "TOUR", copy.getId(), requestInfo);
        return detail(copy, null, true);
    }

    @Transactional
    public TourDetailResponse changeVisibility(Long tourId, ChangeTourVisibilityRequest request,
                                               ClientRequestInfo requestInfo) {
        Long ownerId = currentUserService.requireUserId();
        TourEntity tour = ownedLocked(tourId, ownerId);
        requireVersion(tour, request.version());
        if (tour.getStatus() != TourStatus.PUBLISHED) {
            throw new ConflictException("Only a published tour can be shared");
        }
        tour.changeVisibility(request.visibility());
        auditService.recordDomain(ownerId, "TOUR_VISIBILITY_CHANGED", "TOUR", tourId, requestInfo);
        return detail(tour, null, true);
    }

    @Transactional
    public void delete(Long tourId, Integer version, ClientRequestInfo requestInfo) {
        Long ownerId = currentUserService.requireUserId();
        TourEntity tour = ownedLocked(tourId, ownerId);
        requireVersion(tour, version);
        requireEditable(tour);
        tour.delete(clock.instant());
        auditService.recordDomain(ownerId, "TOUR_DELETED", "TOUR", tourId, requestInfo);
    }

    private TourEntity ownedLocked(Long tourId, Long ownerId) {
        TourEntity tour = tourRepository.findLockedById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour was not found"));
        if (!Objects.equals(tour.getOwnerUserId(), ownerId)) throw new AccessDeniedException("Tour is not owned by this user");
        return tour;
    }

    private void requireEditable(TourEntity tour) {
        if (tour.getStatus() != TourStatus.DRAFT && tour.getStatus() != TourStatus.REJECTED) {
            throw new ConflictException("Tour cannot be edited from its current state");
        }
    }

    private void requireVersion(TourEntity tour, Integer expected) {
        if (expected == null || !Objects.equals(tour.getVersion(), expected)) {
            throw new ConflictException("Tour was changed by another request; reload and try again");
        }
    }

    private List<PlaceEntity> validateItems(List<TourItemRequest> items) {
        Set<Long> ids = items.stream().map(TourItemRequest::placeId).collect(Collectors.toSet());
        if (ids.size() != items.size()) throw new ConflictException("Tour destinations must be unique");
        List<PlaceEntity> places = placeRepository.findAllByIdInAndStatus(ids, PlaceStatus.PUBLISHED);
        if (places.size() != ids.size()) throw new ConflictException("Every tour destination must be a published place");
        return places;
    }

    private void saveItems(Long tourId, List<TourItemRequest> requests) {
        List<TourItemEntity> items = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            TourItemRequest request = requests.get(index);
            items.add(new TourItemEntity(tourId, request.placeId(), index + 1, request.plannedStartAt(),
                    request.durationMinutes(), normalize(request.transportMethod()), normalize(request.note())));
        }
        itemRepository.saveAll(items);
    }

    private TourSummaryResponse summary(TourEntity tour) {
        return new TourSummaryResponse(
                tour.getId(), tour.getTitle(), tour.getDescription(), tour.getRegion(), tour.getDifficultyLevel(),
                tour.getEstimatedDistanceKm(), tour.getEstimatedDurationMinutes(),
                Math.toIntExact(itemRepository.countByTourId(tour.getId())), tour.getStatus(), tour.getVisibility(),
                tour.getSourceTourId(), tour.getVersion(), tour.getUpdatedAt()
        );
    }

    private TourDetailResponse detail(TourEntity tour, List<PlaceEntity> knownPlaces, boolean owner) {
        List<TourItemEntity> items = itemRepository.findAllByTourIdOrderByVisitOrderAsc(tour.getId());
        Set<Long> placeIds = items.stream().map(TourItemEntity::getPlaceId).collect(Collectors.toSet());
        List<PlaceEntity> sourcePlaces = knownPlaces == null ? placeRepository.findAllById(placeIds) : knownPlaces;
        Map<Long, PlaceEntity> places = sourcePlaces.stream().collect(Collectors.toMap(PlaceEntity::getId, Function.identity()));
        String ownerName = userRepository.findById(tour.getOwnerUserId()).map(UserEntity::getDisplayName).orElse("Người dùng LTSS");
        List<TourItemResponse> responses = items.stream().map(item -> {
            PlaceEntity place = places.get(item.getPlaceId());
            return new TourItemResponse(item.getId(), item.getPlaceId(), place == null ? "Địa điểm" : place.getName(),
                    place == null ? null : place.getSlug(), place == null ? null : place.getAddress(),
                    item.getVisitOrder(), item.getPlannedStartAt(), item.getDurationMinutes(),
                    item.getTransportMethod(), item.getNote());
        }).toList();
        return new TourDetailResponse(tour.getId(), tour.getOwnerUserId(), ownerName, tour.getSourceTourId(),
                tour.getTitle(), tour.getDescription(), tour.getRegion(), tour.getDifficultyLevel(),
                tour.getEstimatedDistanceKm(), tour.getEstimatedDurationMinutes(), tour.getStatus(),
                tour.getVisibility(), tour.getVersion(), tour.getPublishedAt(), tour.getCreatedAt(),
                tour.getUpdatedAt(), responses, owner);
    }

    private String copyTitle(String title) {
        String value = "Bản sao - " + title;
        return value.length() <= 200 ? value : value.substring(0, 200);
    }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
