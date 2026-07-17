package com.ltss.features.tour.dto;

import com.ltss.features.tour.entity.TourStatus;
import com.ltss.features.tour.entity.TourVisibility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TourDetailResponse(
        Long id, Long ownerUserId, String ownerDisplayName, Long sourceTourId,
        String title, String description, String region, String difficultyLevel,
        BigDecimal estimatedDistanceKm, Integer estimatedDurationMinutes,
        TourStatus status, TourVisibility visibility, Integer version,
        Instant publishedAt, Instant createdAt, Instant updatedAt,
        List<TourItemResponse> items, boolean ownedByCurrentUser
) {}
