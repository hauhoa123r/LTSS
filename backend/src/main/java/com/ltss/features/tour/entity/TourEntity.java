package com.ltss.features.tour.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "tours")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TourEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;
    @Column(name = "source_tour_id")
    private Long sourceTourId;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(length = 150)
    private String region;
    @Column(name = "difficulty_level", length = 30)
    private String difficultyLevel;
    @Column(name = "estimated_distance_km", precision = 10, scale = 2)
    private BigDecimal estimatedDistanceKm;
    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TourStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TourVisibility visibility;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @Version @Column(nullable = false)
    private Integer version;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TourEntity(Long ownerUserId, Long sourceTourId, String title, String description,
                      String region, String difficultyLevel, BigDecimal distanceKm,
                      Integer durationMinutes) {
        this.ownerUserId = ownerUserId;
        this.sourceTourId = sourceTourId;
        update(title, description, region, difficultyLevel, distanceKm, durationMinutes);
        this.status = TourStatus.DRAFT;
        this.visibility = TourVisibility.PRIVATE;
    }

    public void update(String title, String description, String region, String difficultyLevel,
                       BigDecimal distanceKm, Integer durationMinutes) {
        this.title = title;
        this.description = description;
        this.region = region;
        this.difficultyLevel = difficultyLevel;
        this.estimatedDistanceKm = distanceKm;
        this.estimatedDurationMinutes = durationMinutes;
    }

    public void changeVisibility(TourVisibility visibility) { this.visibility = visibility; }
    public void delete(Instant now) { this.status = TourStatus.DELETED; this.deletedAt = now; }
}
