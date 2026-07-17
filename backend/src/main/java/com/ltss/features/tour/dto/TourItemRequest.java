package com.ltss.features.tour.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record TourItemRequest(
        @NotNull @Min(1) Long placeId,
        Instant plannedStartAt,
        @Min(1) Integer durationMinutes,
        @Size(max = 50) String transportMethod,
        @Size(max = 1000) String note
) {}
