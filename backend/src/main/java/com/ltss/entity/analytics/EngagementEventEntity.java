package com.ltss.entity.analytics;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Entity
@Table(name = "engagement_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngagementEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_type_code", nullable = false, length = 40)
    private String eventTypeCode;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "session_key", nullable = false, length = 100)
    private String sessionKey;
    @Column(name = "place_id") private Long placeId;
    @Column(name = "business_id") private Long businessId;
    @Column(name = "event_id") private Long eventId;
    @Column(name = "article_id") private Long articleId;
    @Column(name = "business_post_id") private Long businessPostId;
    @Column(name = "promotion_id") private Long promotionId;
    @Column(name = "tour_id") private Long tourId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "json")
    private Map<String, String> metadata;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public EngagementEventEntity(String eventTypeCode, Long userId, String sessionKey,
                                 EngagementTargetType targetType, Long targetId,
                                 Map<String, String> metadata, Instant occurredAt) {
        this.eventTypeCode = eventTypeCode;
        this.userId = userId;
        this.sessionKey = sessionKey;
        this.metadata = metadata == null || metadata.isEmpty() ? null : Map.copyOf(metadata);
        this.occurredAt = occurredAt;
        assignTarget(targetType, targetId);
    }

    private void assignTarget(EngagementTargetType type, Long id) {
        switch (type) {
            case PLACE -> placeId = id;
            case BUSINESS -> businessId = id;
            case EVENT -> eventId = id;
            case ARTICLE -> articleId = id;
            case BUSINESS_POST -> businessPostId = id;
            case PROMOTION -> promotionId = id;
            case TOUR -> tourId = id;
        }
    }
}
