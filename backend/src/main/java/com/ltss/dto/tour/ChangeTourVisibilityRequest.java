package com.ltss.dto.tour;

import com.ltss.entity.tour.TourVisibility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChangeTourVisibilityRequest(
        @NotNull TourVisibility visibility,
        @NotNull @Min(0) Integer version
) {}
