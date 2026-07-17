package com.ltss.features.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "quiz_badges")
@IdClass(QuizBadgeId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizBadgeEntity {
    @Id @Column(name = "quiz_id")
    private Long quizId;
    @Id @Column(name = "badge_id")
    private Long badgeId;
    @Column(name = "minimum_score_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal minimumScorePercent;
}
