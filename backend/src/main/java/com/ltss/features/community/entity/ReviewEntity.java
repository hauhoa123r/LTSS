package com.ltss.features.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "place_id") private Long placeId;
    @Column(name = "business_id") private Long businessId;
    @Column(name = "article_id") private Long articleId;
    @Column(name = "tour_id") private Long tourId;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private Short rating;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ReviewStatus status;
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Version @Column(nullable = false)
    private Integer version;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ReviewEntity(Long userId, ReviewTargetType type, Long targetId, int rating, String comment, Instant now) {
        this.userId = userId;
        this.rating = (short) rating;
        this.comment = comment;
        this.status = ReviewStatus.PENDING;
        this.submittedAt = now;
        assignTarget(type, targetId);
    }

    public ReviewTargetType targetType() {
        if (placeId != null) return ReviewTargetType.PLACE;
        if (businessId != null) return ReviewTargetType.BUSINESS;
        if (articleId != null) return ReviewTargetType.ARTICLE;
        return ReviewTargetType.TOUR;
    }

    public Long targetId() {
        return switch (targetType()) {
            case PLACE -> placeId;
            case BUSINESS -> businessId;
            case ARTICLE -> articleId;
            case TOUR -> tourId;
        };
    }

    public void approve(Instant now) { status = ReviewStatus.VISIBLE; publishedAt = now; }
    public void reject() { status = ReviewStatus.REJECTED; }
    public void cancelSubmission() { status = ReviewStatus.REMOVED; }

    private void assignTarget(ReviewTargetType type, Long id) {
        switch (type) {
            case PLACE -> placeId = id;
            case BUSINESS -> businessId = id;
            case ARTICLE -> articleId = id;
            case TOUR -> tourId = id;
        }
    }
}
