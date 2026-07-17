package com.ltss.features.tour.dto;

import com.ltss.features.tour.entity.TourVisibility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChangeTourVisibilityRequest(
        @NotNull TourVisibility visibility,
        @NotNull @Min(0) Integer version
) {}
