package com.ltss.dto.content;

import com.ltss.entity.place.PlaceEntity;

public record LinkedPlaceResponse(Long id, String name, String slug, String address) {
    public static LinkedPlaceResponse from(PlaceEntity place) {
        return new LinkedPlaceResponse(place.getId(), place.getName(), place.getSlug(), place.getAddress());
    }
}
