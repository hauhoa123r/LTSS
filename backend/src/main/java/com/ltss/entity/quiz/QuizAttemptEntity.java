package com.ltss.entity.quiz;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "quiz_attempts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttemptEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "quiz_id", nullable = false)
    private Long quizId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25)
    private QuizAttemptStatus status;
    @Column(name = "randomization_seed", length = 64)
    private String randomizationSeed;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal score;
    @Column(name = "total_points", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalPoints;
    @Column(name = "score_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal scorePercent;
    @Column(name = "is_passed", nullable = false)
    private boolean passed;
    @Column(name = "location_verified_at")
    private Instant locationVerifiedAt;
    @Column(name = "distance_to_place_meters", precision = 10, scale = 2)
    private BigDecimal distanceToPlaceMeters;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public QuizAttemptEntity(Long quizId, Long userId, String seed, Instant startedAt,
                             Instant expiresAt, BigDecimal totalPoints, BigDecimal distanceMeters) {
        this.quizId = quizId;
        this.userId = userId;
        this.status = QuizAttemptStatus.IN_PROGRESS;
        this.randomizationSeed = seed;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.score = BigDecimal.ZERO.setScale(2);
        this.totalPoints = totalPoints;
        this.scorePercent = BigDecimal.ZERO.setScale(2);
        this.passed = false;
        this.locationVerifiedAt = startedAt;
        this.distanceToPlaceMeters = distanceMeters;
        this.createdAt = startedAt;
    }

    public void complete(QuizAttemptStatus terminalStatus, Instant now, BigDecimal score,
                         BigDecimal scorePercent, boolean passed) {
        this.status = terminalStatus;
        this.submittedAt = now;
        this.score = score;
        this.scorePercent = scorePercent;
        this.passed = passed;
    }
}
