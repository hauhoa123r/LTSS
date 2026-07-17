package com.ltss.dto.quiz;

import com.ltss.entity.quiz.QuizAttemptStatus;

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
