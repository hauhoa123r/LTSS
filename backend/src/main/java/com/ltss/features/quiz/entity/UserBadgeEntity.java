package com.ltss.features.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "user_badges")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBadgeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "badge_id", nullable = false)
    private Long badgeId;
    @Column(name = "awarded_by_quiz_id")
    private Long awardedByQuizId;
    @Column(name = "awarded_attempt_id")
    private Long awardedAttemptId;
    @Column(name = "awarded_at", nullable = false)
    private Instant awardedAt;

    public UserBadgeEntity(Long userId, Long badgeId, Long quizId, Long attemptId, Instant awardedAt) {
        this.userId = userId;
        this.badgeId = badgeId;
        this.awardedByQuizId = quizId;
        this.awardedAttemptId = attemptId;
        this.awardedAt = awardedAt;
    }
}
