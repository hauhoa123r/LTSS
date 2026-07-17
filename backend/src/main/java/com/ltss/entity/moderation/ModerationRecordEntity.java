package com.ltss.entity.moderation;

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
@Table(name = "moderation_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModerationRecordEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "submitted_by_user_id")
    private Long submittedByUserId;
    @Column(name = "moderator_user_id")
    private Long moderatorUserId;
    @Column(name = "place_id") private Long placeId;
    @Column(name = "business_id") private Long businessId;
    @Column(name = "event_id") private Long eventId;
    @Column(name = "article_id") private Long articleId;
    @Column(name = "business_post_id") private Long businessPostId;
    @Column(name = "promotion_id") private Long promotionId;
    @Column(name = "tour_id") private Long tourId;
    @Column(name = "quiz_id") private Long quizId;
    @Column(name = "review_id") private Long reviewId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_snapshot", columnDefinition = "JSON")
    private Map<String, Object> targetSnapshot;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ModerationStatus status;
    @Enumerated(EnumType.STRING) @Column(length = 20)
    private ModerationDecision decision;
    @Column(name = "submission_note", length = 1000)
    private String submissionNote;
    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public static ModerationRecordEntity pending(
            Long submitterId,
            ModerationTargetType targetType,
            Long targetId,
            String note,
            Map<String, Object> targetSnapshot,
            Instant now
    ) {
        ModerationRecordEntity record = new ModerationRecordEntity();
        record.submittedByUserId = submitterId;
        record.status = ModerationStatus.PENDING;
        record.submissionNote = note;
        record.targetSnapshot = targetSnapshot;
        record.submittedAt = now;
        record.assignTarget(targetType, targetId);
        return record;
    }

    public void resolve(Long moderatorId, ModerationDecision decision, String reason, Instant now) {
        this.moderatorUserId = moderatorId;
        this.status = ModerationStatus.RESOLVED;
        this.decision = decision;
        this.decisionReason = reason;
        this.resolvedAt = now;
    }

    public void cancel(Instant now) {
        this.status = ModerationStatus.CANCELLED;
        this.resolvedAt = now;
    }

    public ModerationTargetType targetType() {
        if (articleId != null) return ModerationTargetType.ARTICLE;
        if (eventId != null) return ModerationTargetType.EVENT;
        if (businessPostId != null) return ModerationTargetType.BUSINESS_POST;
        if (promotionId != null) return ModerationTargetType.PROMOTION;
        if (quizId != null) return ModerationTargetType.QUIZ;
        if (reviewId != null) return ModerationTargetType.REVIEW;
        return null;
    }

    public Long targetId() {
        return switch (targetType()) {
            case ARTICLE -> articleId;
            case EVENT -> eventId;
            case BUSINESS_POST -> businessPostId;
            case PROMOTION -> promotionId;
            case QUIZ -> quizId;
            case REVIEW -> reviewId;
            case null -> null;
        };
    }

    private void assignTarget(ModerationTargetType type, Long id) {
        switch (type) {
            case ARTICLE -> articleId = id;
            case EVENT -> eventId = id;
            case BUSINESS_POST -> businessPostId = id;
            case PROMOTION -> promotionId = id;
            case QUIZ -> quizId = id;
            case REVIEW -> reviewId = id;
        }
    }
}
