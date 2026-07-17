package com.ltss.features.place.dto;

import com.ltss.features.place.entity.PlaceCategoryEntity;

public record PlaceCategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        String markerIconKey
) {
    public static PlaceCategoryResponse from(PlaceCategoryEntity category) {
        return new PlaceCategoryResponse(
                category.getId(),
                category.getCategoryName(),
                category.getSlug(),
                category.getDescription(),
                category.getMarkerIconKey()
        );
    }
}
