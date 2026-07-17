package com.ltss.dto.quiz;

import com.ltss.entity.quiz.QuizAttemptStatus;

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
