package com.ltss.dto.quiz;

import com.ltss.entity.quiz.QuizStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record QuizSummaryResponse(
        Long id,
        Long placeId,
        String placeName,
        String title,
        String description,
        int timeLimitSeconds,
        BigDecimal passingScorePercent,
        int questionCount,
        QuizStatus status,
        Integer version,
        Instant publishedAt,
        Instant updatedAt
) {}
