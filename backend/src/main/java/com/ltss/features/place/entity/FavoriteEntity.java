package com.ltss.features.place.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "favorites")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteEntity {
    @EmbeddedId
    private FavoriteId id;

    public FavoriteEntity(Long userId, Long placeId) {
        id = new FavoriteId(userId, placeId);
    }
}
