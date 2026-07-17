package com.ltss.entity.quiz;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "quizzes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "place_id", nullable = false)
    private Long placeId;
    @Column(name = "created_by_user_id")
    private Long createdByUserId;
    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;
    @Column(nullable = false, length = 250)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "time_limit_seconds", nullable = false)
    private Integer timeLimitSeconds;
    @Column(name = "passing_score_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal passingScorePercent;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private QuizStatus status;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @Version @Column(nullable = false)
    private Integer version;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public QuizEntity(Long placeId, Long creatorId, String title, String description,
                      Integer timeLimitSeconds, BigDecimal passingScorePercent) {
        this.placeId = placeId;
        this.createdByUserId = creatorId;
        this.status = QuizStatus.DRAFT;
        update(creatorId, title, description, timeLimitSeconds, passingScorePercent);
    }

    public void update(Long actorId, String title, String description,
                       Integer timeLimitSeconds, BigDecimal passingScorePercent) {
        this.updatedByUserId = actorId;
        this.title = title;
        this.description = description;
        this.timeLimitSeconds = timeLimitSeconds;
        this.passingScorePercent = passingScorePercent;
    }

    public void submit(Instant now) {
        status = QuizStatus.PENDING;
        submittedAt = now;
        publishedAt = null;
    }

    public void approve(Instant now) {
        status = QuizStatus.PUBLISHED;
        publishedAt = now;
    }

    public void reject() { status = QuizStatus.REJECTED; }
    public void cancelSubmission() { status = QuizStatus.DRAFT; submittedAt = null; }
    public void delete(Instant now) { status = QuizStatus.DELETED; deletedAt = now; }
}
