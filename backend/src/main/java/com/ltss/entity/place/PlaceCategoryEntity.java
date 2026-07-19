package com.ltss.entity.place;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "place_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "marker_icon_key", length = 100)
    private String markerIconKey;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private Long createdByUserId;

    @Column(name = "updated_at", nullable = false)
    private java.time.Instant updatedAt;

    @Column(name = "updated_by_user_id", nullable = false)
    private Long updatedByUserId;

    public PlaceCategoryEntity(String categoryName, String slug, String description, boolean active, Long actorId) {
        this.categoryName = categoryName;
        this.slug = slug;
        this.description = description;
        this.active = active;
        this.createdAt = java.time.Instant.now();
        this.createdByUserId = actorId;
        this.updatedAt = this.createdAt;
        this.updatedByUserId = actorId;
    }

    public void update(String categoryName, String slug, String description, boolean active, Long actorId) {
        this.categoryName = categoryName;
        this.slug = slug;
        this.description = description;
        this.active = active;
        this.updatedAt = java.time.Instant.now();
        this.updatedByUserId = actorId;
    }

    public void deactivate(Long actorId) {
        this.active = false;
        this.updatedAt = java.time.Instant.now();
        this.updatedByUserId = actorId;
    }
}
