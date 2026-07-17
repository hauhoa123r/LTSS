package com.ltss.dto.tour;

import com.ltss.entity.tour.TourStatus;
import com.ltss.entity.tour.TourVisibility;

import java.math.BigDecimal;
import java.time.Instant;

public record TourSummaryResponse(
        Long id, String title, String description, String region, String difficultyLevel,
        BigDecimal estimatedDistanceKm, Integer estimatedDurationMinutes, int stopCount,
        TourStatus status, TourVisibility visibility, Long sourceTourId,
        Integer version, Instant updatedAt
) {}
