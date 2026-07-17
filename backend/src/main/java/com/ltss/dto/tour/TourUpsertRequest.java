package com.ltss.dto.tour;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record TourUpsertRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @Size(max = 150) String region,
        @Size(max = 30) String difficultyLevel,
        @DecimalMin("0.0") BigDecimal estimatedDistanceKm,
        @Min(1) Integer estimatedDurationMinutes,
        @NotNull @Size(min = 2, max = 10) List<@Valid TourItemRequest> items,
        @Min(0) Integer version
) {}
