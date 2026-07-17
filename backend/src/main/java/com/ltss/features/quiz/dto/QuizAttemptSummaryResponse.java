package com.ltss.features.quiz.dto;

import com.ltss.features.quiz.entity.QuizAttemptStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record QuizAttemptSummaryResponse(
        Long id,
        Long quizId,
        String quizTitle,
        QuizAttemptStatus status,
        Instant startedAt,
        Instant submittedAt,
        BigDecimal scorePercent,
        boolean passed
) {}
