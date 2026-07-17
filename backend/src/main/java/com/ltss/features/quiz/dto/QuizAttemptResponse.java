package com.ltss.features.quiz.dto;

import com.ltss.features.quiz.entity.QuizAttemptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QuizAttemptResponse(
        Long id,
        Long quizId,
        String quizTitle,
        QuizAttemptStatus status,
        Instant startedAt,
        Instant expiresAt,
        Instant submittedAt,
        BigDecimal distanceToPlaceMeters,
        BigDecimal score,
        BigDecimal totalPoints,
        BigDecimal scorePercent,
        boolean passed,
        List<AttemptQuestionResponse> questions,
        List<AwardedBadgeResponse> awardedBadges
) {}
