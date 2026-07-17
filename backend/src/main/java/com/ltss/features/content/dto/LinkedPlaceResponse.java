package com.ltss.features.content.dto;

import com.ltss.features.place.entity.PlaceEntity;

public record LinkedPlaceResponse(Long id, String name, String slug, String address) {
    public static LinkedPlaceResponse from(PlaceEntity place) {
        return new LinkedPlaceResponse(place.getId(), place.getName(), place.getSlug(), place.getAddress());
    }
}
