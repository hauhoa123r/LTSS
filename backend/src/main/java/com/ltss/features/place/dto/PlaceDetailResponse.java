package com.ltss.features.place.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlaceDetailResponse(
        Long id,
        String name,
        String slug,
        String summary,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String openingHours,
        BigDecimal entranceFee,
        String contactPhone,
        PlaceCategoryResponse category,
        RelicDetailResponse relicDetail,
        List<PlaceMediaResponse> media,
        boolean favorite,
        Integer version
) {
}
