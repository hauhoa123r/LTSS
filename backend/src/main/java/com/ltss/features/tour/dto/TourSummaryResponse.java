package com.ltss.features.tour.dto;

import com.ltss.features.tour.entity.TourStatus;
import com.ltss.features.tour.entity.TourVisibility;

import java.math.BigDecimal;
import java.time.Instant;

public record TourSummaryResponse(
        Long id, String title, String description, String region, String difficultyLevel,
        BigDecimal estimatedDistanceKm, Integer estimatedDurationMinutes, int stopCount,
        TourStatus status, TourVisibility visibility, Long sourceTourId,
        Integer version, Instant updatedAt
) {}
