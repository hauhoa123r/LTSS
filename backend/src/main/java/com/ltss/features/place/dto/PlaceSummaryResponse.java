package com.ltss.features.place.dto;

import java.math.BigDecimal;

public record PlaceSummaryResponse(
        Long id,
        String name,
        String slug,
        String summary,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal entranceFee,
        PlaceCategoryResponse category,
        String coverUrl,
        Double distanceKm,
        boolean favorite
) {
}
