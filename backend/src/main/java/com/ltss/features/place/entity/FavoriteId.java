package com.ltss.features.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteId implements Serializable {
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "place_id")
    private Long placeId;
}
